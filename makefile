all:
	javac -g com/CommonProperties.java
	javac -g com/Server.java
	javac -g logger/Logger.java
	javac -g message/Handshake.java
	javac -g message/Message.java
	javac -g message/MessageAttributes.java
	javac -g message/MessageConstants.java
	javac -g message/MessageProcessor.java
	javac -g payloads/BitFieldPayload.java
	javac -g payloads/PiecePayload.java
	javac -g peer/PeerDataRateComparator.java
	javac -g peer/PeerProcess.java
	javac -g peer/RemotePeerHandler.java
	javac -g peer/RemotePeerInfo.java
	javac -g util/ConversionUtil.java
	javac -g util/DateUtil.java
	javac -g StartRemotePeers.java

clean:
	find . -type f -name '*.class' -delete
