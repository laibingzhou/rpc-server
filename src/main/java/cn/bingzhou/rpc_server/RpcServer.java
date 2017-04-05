package cn.bingzhou.rpc_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import cn.bingzhou.rpc.common.RpcDecoder;
import cn.bingzhou.rpc.common.RpcEncoder;
import cn.bingzhou.rpc.common.RpcRequest;
import cn.bingzhou.rpc.common.RpcResponse;
import cn.bingzhou.rpcregister.ServiceRegister;

public class RpcServer implements ApplicationContextAware,InitializingBean {
	
	
	private Logger logger=LoggerFactory.getLogger(RpcServer.class);
	
	public String address;
	
	public ServiceRegister register;
	
	private volatile Map<String,Object> handlerMap=new HashMap<String, Object>();

	public RpcServer(String address, ServiceRegister register) {
		super();
		this.address = address;
		this.register = register;
	}
	
	
	/**
	 * 启动Netty服务器，先解码，后接受到发送数据后解码，接着
	 */
	public void afterPropertiesSet() throws Exception {
		ServerBootstrap sb=new ServerBootstrap();
		EventLoopGroup parentGroup=new NioEventLoopGroup();
		EventLoopGroup childGroup=new NioEventLoopGroup();
		sb.group(parentGroup, childGroup)
		  .channel(NioServerSocketChannel.class)
		  .childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline().addLast(new RpcDecoder(RpcRequest.class))
								  .addLast(new RpcEncoder(RpcResponse.class))
				                  .addLast(new RpcHandler(handlerMap));
			}
			  
		}).option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true);
		String[] split = address.split(":");
		String ip=split[1];
		int port=Integer.parseInt(split[1]);
		ChannelFuture sync = sb.bind(ip, port).sync();
		logger.debug("netty服务器已经绑定到了{}:{}",ip,port);
		if(register!=null){
			register.register(address);
		}
	}

	public void setApplicationContext(ApplicationContext ctx)
			throws BeansException {
		Map<String, Object> beansWithAnnotation = ctx.getBeansWithAnnotation(RpcService.class);
		for(Object serviceBean:beansWithAnnotation.values()){
			String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
			handlerMap.put(interfaceName, serviceBean);
		}
	}
	
	

}
