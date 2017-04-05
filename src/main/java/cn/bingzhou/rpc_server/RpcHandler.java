package cn.bingzhou.rpc_server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.bingzhou.rpc.common.RpcRequest;
import cn.bingzhou.rpc.common.RpcResponse;

public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
	
	private Logger logger=Logger.getLogger(this.getClass());
	
	private Map<String,Object> handlerMap=new HashMap<String, Object>();

	public RpcHandler(Map<String, Object> handlerMap) {
		this.handlerMap=handlerMap;
	}

	/**
	 * 利用发射执行所需要的数据
	 */
	@Override
	protected void channelRead0(
			ChannelHandlerContext ctx, RpcRequest request)
			throws Exception {
		//利用反射执行请求的方法
		RpcResponse rpcResponse=new RpcResponse();
		try{
			String interfaceName = request.getInterfaceName();
			String methodName = request.getMethodName();
			rpcResponse.setResponseId(request.getRequestId());
			Object object = handlerMap.get(interfaceName);
			if(object!=null){
				Method method = object.getClass().getMethod(methodName, request.getClss());
				method.setAccessible(true);
				Object returnObj = method.invoke(object, request.getParams());
				rpcResponse.setObj(returnObj);
			}else{
				logger.info("接口不存在："+methodName);
			}
		}catch(Exception e){
			rpcResponse.setError(true);
			rpcResponse.setErrorMsg(e.getMessage());
		}
		ctx.writeAndFlush(rpcResponse);
	}

}
