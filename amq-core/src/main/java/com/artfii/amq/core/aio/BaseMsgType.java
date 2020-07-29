package com.artfii.amq.core.aio;

/**
 * 定义消息类型
 * 
 * @author 三刀
 * @version MessageType.java, v 0.1 2015年8月24日 上午9:37:04 Seer Exp.
 */
public interface BaseMsgType {
	/** 请求消息类型 */
	int REQUEST_MESSAGE = 0x10000000;
	/** 响应消息类型 */
	int RESPONSE_MESSAGE = 0x11000000;

	/** 心跳消息REQ */
	int HEART_MESSAGE_REQ = REQUEST_MESSAGE | 0x1;

	/** 心跳消息RSP */
	int HEART_MESSAGE_RSP = RESPONSE_MESSAGE | 0x1;

	/** 鉴权请求 */
	int LOGIN_AUTH_REQ = REQUEST_MESSAGE | 0x2;

	/** 鉴权响应 */
	int LOGIN_AUTH_RSP = RESPONSE_MESSAGE | 0x2;

	/** 断链重连请求 */
	int RE_CONNECT_REQ = REQUEST_MESSAGE | 0x3;

	/** 断链重连响应 */
	int RE_CONNECT_RSP = RESPONSE_MESSAGE | 0x3;

	/** 探测消息REQ */
	int DETECT_MESSAGE_REQ = REQUEST_MESSAGE | 0x4;

	/** 探测消息RSP */
	int DETECT_MESSAGE_RSP = RESPONSE_MESSAGE | 0x4;

	/** 无效消息请求 */
	int INVALID_MESSAGE_REQ = REQUEST_MESSAGE | 0x5;

	/** 无效消息响应 */
	int INVALID_MESSAGE_RSP = RESPONSE_MESSAGE | 0x5;

	/** 集群业务请求消息 */
	int CLUSTER_MESSAGE_REQ = REQUEST_MESSAGE | 0x06;

	/** 集群业务响应消息 */
	int CLUSTER_MESSAGE_RSP = RESPONSE_MESSAGE | 0x06;

	/** 字节流请求消息 */
	int BYTE_ARRAY_MESSAGE_REQ = REQUEST_MESSAGE | 0x07;

	/** 字节流响应消息 */
	int BYTE_ARRAY_MESSAGE_RSP = RESPONSE_MESSAGE | 0x07;

	/** 远程接口请求消息 */
	int REMOTE_INTERFACE_MESSAGE_REQ = REQUEST_MESSAGE | 0x08;

	/** 远程接口响应消息 */
	int REMOTE_INTERFACE_MESSAGE_RSP = RESPONSE_MESSAGE | 0x08;

	/** 安全通信请求消息 */
	int SECURE_SOCKET_MESSAGE_REQ = REQUEST_MESSAGE | 0x09;

	/** 安全通信响应消息 */
	int SECURE_SOCKET_MESSAGE_RSP = RESPONSE_MESSAGE | 0x09;
}
