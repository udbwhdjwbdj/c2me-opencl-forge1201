package com.ishland.c2me.opts.dfc.common.gen.opencl;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF32;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

public interface OpenCLCGenContext {
   String signature = "(const sample_int32_ctx_t ctx)";
   String filler = "nop();".repeat(200) + "\n";

   String nextMethodName();

   String nextMethodName(String var1);

   ValuesMethodDef newDispatcher(AstNode var1, String var2, AstNode.ReturnType var3);

   ValuesMethodDefF64 newDispatcherF64(AstNode var1);

   ValuesMethodDefF64 newDispatcherF64(AstNode var1, String var2);

   ValuesMethodDefF32 newDispatcherF32(AstNode var1);

   ValuesMethodDefF32 newDispatcherF32(AstNode var1, String var2);

   ValuesMethodDef newMethod(AstNode var1, OpenCLCGenFunctionContext.FunctionVariant var2, AstNode.ReturnType var3);

   ValuesMethodDefF64 newMethodF64(AstNode var1, OpenCLCGenFunctionContext.FunctionVariant var2);

   ValuesMethodDefF32 newMethodF32(AstNode var1, OpenCLCGenFunctionContext.FunctionVariant var2);

   String callDelegate(ValuesMethodDef var1, AstNode.ReturnType var2);

   String callDelegate(ValuesMethodDefF64 var1);

   String callDelegate(ValuesMethodDefF32 var1);

   int allocGlobalDynamicData(Object var1);

   int allocGlobalConstData(byte[] var1, int var2);

   int allocGlobalConstDataObject(Object var1);

   int getGlobalDynamicDataOffset(Object var1);

   int registerFlatCache(CacheLikeNode var1);

   int registerCache2d(CacheLikeNode var1);

   int registerInterpolator(CacheLikeNode var1);

   void appendRaw(String var1);
}
