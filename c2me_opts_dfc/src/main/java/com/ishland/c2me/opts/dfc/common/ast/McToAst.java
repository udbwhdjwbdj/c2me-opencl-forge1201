package com.ishland.c2me.opts.dfc.common.ast;

import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.conversion.ToF64Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FastCacheLikeAdapter;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FindTopSurfaceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.Multi2SingleNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.gen.backports.FindTopSurface;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.DensityFunctions.Ap2;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierMarker;
import net.minecraft.world.level.levelgen.DensityFunctions.BlendDensity;
import net.minecraft.world.level.levelgen.DensityFunctions.Clamp;
import net.minecraft.world.level.levelgen.DensityFunctions.EndIslandDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.HolderHolder;
import net.minecraft.world.level.levelgen.DensityFunctions.Mapped;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker;
import net.minecraft.world.level.levelgen.DensityFunctions.MulOrAdd;
import net.minecraft.world.level.levelgen.DensityFunctions.Noise;
import net.minecraft.world.level.levelgen.DensityFunctions.RangeChoice;
import net.minecraft.world.level.levelgen.DensityFunctions.Shift;
import net.minecraft.world.level.levelgen.DensityFunctions.ShiftA;
import net.minecraft.world.level.levelgen.DensityFunctions.ShiftB;
import net.minecraft.world.level.levelgen.DensityFunctions.ShiftedNoise;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Coordinate;
import net.minecraft.world.level.levelgen.DensityFunctions.Spline.Point;
import net.minecraft.world.level.levelgen.DensityFunctions.TwoArgumentSimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.WeirdScaledSampler;
import net.minecraft.world.level.levelgen.DensityFunctions.YClampedGradient;
import net.minecraft.world.level.levelgen.NoiseChunk.Cache2D;
import net.minecraft.world.level.levelgen.NoiseChunk.CacheOnce;
import net.minecraft.world.level.levelgen.NoiseChunk.FlatCache;
import net.minecraft.world.level.levelgen.NoiseChunk.NoiseInterpolator;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McToAst {
   private static final Logger LOGGER = LoggerFactory.getLogger(McToAst.class);
   public static final FrontendRegistry<AstEmitter<? extends DensityFunction>> REGISTRY = new FrontendRegistry<>();
   private static final ConcurrentHashMap<Class<?>, AtomicLong> delegateStatistics = new ConcurrentHashMap<>();

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static AstNode toAstSpline(CubicSpline<Point, Coordinate> spline) {
      Objects.requireNonNull(spline);

      if (spline instanceof CubicSpline.Constant<Point, Coordinate> f) {
         return new ConstantF32Node(f.value());
      }
      if (spline instanceof CubicSpline.Multipoint<Point, Coordinate> fx) {
         return new SplineNormalNode(
            toAst(((Coordinate) fx.coordinate()).function().value()),
            (float[]) fx.locations().clone(),
            ((List<CubicSpline<Point, Coordinate>>) (List<?>) fx.values()).stream()
               .map(McToAst::toAstSpline)
               .toArray(AstNode[]::new),
            (float[]) fx.derivatives().clone()
         );
      }
      throw new IllegalStateException("Unexpected type: " + spline.getClass());
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static <T extends DensityFunction> AstNode toAst(T df) {
      AstEmitter emitter = REGISTRY.getOptional(df.getClass());
      if (emitter != null) {
         return emitter.toAst(df);
      } else {
         long known = delegateStatistics.computeIfAbsent(df.getClass(), unused -> new AtomicLong(0L)).getAndIncrement();
         if (known == 0L) {
            LOGGER.warn("warn_once: Generating DelegateNode for type: {}", df.getClass().toString());
         }
         return new DelegateNode(df);
      }
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static <N extends DensityFunction, E1 extends AstEmitter<N>> void register(Class<N> clazz, E1 emitter) {
      ((FrontendRegistry) REGISTRY).registerExactMatch(clazz, emitter);
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static void registerRaw(Class clazz, AstEmitter emitter) {
      ((FrontendRegistry) REGISTRY).registerExactMatch(clazz, emitter);
   }

   static {
      register(DensityFunctions.BlendAlpha.class, f -> new ConstantNode(1.0));
      register(DensityFunctions.BlendOffset.class, f -> new ConstantNode(0.0));
      AstEmitter<TwoArgumentSimpleFunction> emitter = f -> (AstNode) (switch (f.type()) {
            case ADD -> new AddNode(toAst(f.argument1()), toAst(f.argument2()));
            case MUL -> new MulNode(toAst(f.argument1()), toAst(f.argument2()));
            case MIN -> {
               double rightMin = f.argument2().minValue();
               yield f.argument1().minValue() < rightMin
                  ? new MinShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMin)
                  : new MinNode(toAst(f.argument1()), toAst(f.argument2()));
            }
            case MAX -> {
               double rightMax = f.argument2().maxValue();
               yield f.argument1().maxValue() > rightMax
                  ? new MaxShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMax)
                  : new MaxNode(toAst(f.argument1()), toAst(f.argument2()));
            }
            default -> throw new IllegalStateException("Unexpected type");
         });
      registerRaw(Ap2.class, emitter);
      registerRaw(MulOrAdd.class, emitter);
      register(BlendDensity.class, f -> toAst(f.input()));
      register(Clamp.class, f -> new MaxNode(new ConstantNode(f.minValue()), new MinNode(new ConstantNode(f.maxValue()), toAst(f.input()))));
      register(DensityFunctions.Constant.class, f -> new ConstantNode(f.value()));
      register(HolderHolder.class, f -> toAst(f.function().value()));
      register(Mapped.class, f -> (AstNode) (switch (f.type()) {
            case ABS -> new AbsNode(toAst(f.input()));
            case SQUARE -> new SquareNode(toAst(f.input()));
            case CUBE -> new CubeNode(toAst(f.input()));
            case HALF_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.5);
            case QUARTER_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.25);
            case SQUEEZE -> new SqueezeNode(toAst(f.input()));
            default -> throw new IllegalStateException("Unexpected type");
         }));
      register(RangeChoice.class, f -> new RangeChoiceNode(toAst(f.input()), f.minInclusive(), f.maxExclusive(), toAst(f.whenInRange()), toAst(f.whenOutOfRange())));
      AstEmitter<net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked> markerEmitter = f -> {
         DensityFunction wrapped = f.wrapped();
         return new CacheLikeNode(new FastCacheLikeAdapter(f), toAst(wrapped));
      };
      registerRaw(Marker.class, markerEmitter);
      registerRaw(Cache2D.class, markerEmitter);
      registerRaw(CacheOnce.class, markerEmitter);
      registerRaw(NoiseInterpolator.class, markerEmitter);
      registerRaw(FlatCache.class, markerEmitter);
      register(ShiftedNoise.class, f -> new GenericShiftedNoiseNode(
            new AddNode(new MulNode(CoordinateNode.AXIS_X, new ConstantNode(f.xzScale())), toAst(f.shiftX())),
            new AddNode(new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(f.yScale())), toAst(f.shiftY())),
            new AddNode(new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(f.xzScale())), toAst(f.shiftZ())),
            f.noise()
         )
      );
      register(Noise.class, f -> new GenericShiftedNoiseNode(
            new MulNode(CoordinateNode.AXIS_X, new ConstantNode(f.xzScale())),
            new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(f.yScale())),
            new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(f.xzScale())),
            f.noise()
         )
      );
      register(Shift.class, f -> new MulNode(
            new GenericShiftedNoiseNode(
               new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
               new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(0.25)),
               new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
               f.offsetNoise()
            ),
            new ConstantNode(4.0)
         )
      );
      register(ShiftA.class, f -> new MulNode(
            new GenericShiftedNoiseNode(
               new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
               new ConstantNode(0.0),
               new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
               f.offsetNoise()
            ),
            new ConstantNode(4.0)
         )
      );
      register(ShiftB.class, f -> new MulNode(
            new GenericShiftedNoiseNode(
               new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
               new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
               new ConstantNode(0.0),
               f.offsetNoise()
            ),
            new ConstantNode(4.0)
         )
      );
      register(YClampedGradient.class, f -> new YClampedGradientNode(f.fromY(), f.toY(), f.fromValue(), f.toValue()));
      register(WeirdScaledSampler.class, f -> new DFTWeirdScaledSamplerNode(toAst(f.input()), f.noise(), f.rarityValueMapper()));
      register(Spline.class, f -> new Multi2SingleNode(new ToF64Node(toAstSpline(f.spline()))));
      register(FindTopSurface.class, f -> new FindTopSurfaceNode(toAst(f.delegate()), toAst(f.upperBound()), new ConstantNode(f.lowerBound()), f.cellHeight()));
      register(EndIslandDensityFunction.class, EndIslandsNode::new);
      register(BlendedNoise.class, InterpolatedNoiseSamplerNode::new);
      register(BeardifierMarker.class, BeardifierNode::new);
   }
}
