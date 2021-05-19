package message;

import peer.PeerProcess;
import util.ConversionUtil;

import java.io.UnsupportedEncodingException;

public class Message implements MessageConstants {
    private String messageType;
    private String messageLength;
    private int dataLength = MESSAGEType;
    private byte[] messageTypeArray = null;
    private byte[] lengthsArray = null;
    private byte[] payloadArray = null;

    public Message(String messageTypeArray) {
        try {
            if (messageTypeArray.equals(CHOKEPayload) || messageTypeArray.equals(UNCHOKEPayload)
                    || messageTypeArray.equals(INTERESTEDPayload)
                    || messageTypeArray.equals(NOT_INTERESTEDPayload)) {
                this.setMessageType(messageTypeArray);
                this.payloadArray = null;
                this.setMessageLength(1);
            }
            else
                throw new Exception("Wrong message.Message chosen");
        } catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
        }
    }



    public Message(String messageTypeArray, byte[] payloadArray) {
        try {
            if (payloadArray != null) {
                this.setMessageLength(payloadArray.length + 1);
                if (this.lengthsArray.length > MESSAGELength)
                    throw new Exception("message.Message length out of bounds");
                this.setPayloadArray(payloadArray);
            }
            else {
                if (messageTypeArray.equals(CHOKEPayload) || messageTypeArray.equals(UNCHOKEPayload)
                        || messageTypeArray.equals(INTERESTEDPayload)
                        || messageTypeArray.equals(NOT_INTERESTEDPayload)) {
                    this.setMessageLength(1);
                    this.payloadArray = null;
                }
                else
                    throw new Exception("Null payload");
            }

            this.setMessageType(messageTypeArray);
            if (this.getMessageType().length > MESSAGEType)
                throw new Exception("message.Message length out of bounds");
        } catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
        }
    }

    public Message(){}

    public byte[] getMessageLength() {
        return lengthsArray;
    }

    public int getMessageLengthInt() {
        return this.dataLength;
    }

    public byte[] getMessageType() {return messageTypeArray;}

    public byte[] getPayloadArray() {
        return payloadArray;
    }

    public String getMessageTypeString() {
        return messageType;
    }

    public void setMessageLength(byte[] lengthArray) {
        int len = ConversionUtil.byteArrayToIntegerConverter(lengthArray);
        this.messageLength = Integer.toString(len);
        this.lengthsArray = lengthArray;
        this.dataLength = len;
    }

    public void setMessageLength(int messageLength) {
        this.dataLength = messageLength;
        this.messageLength = ((Integer)messageLength).toString();
        this.lengthsArray = ConversionUtil.integerToByteConverter(messageLength);
    }

    public void setMessageType(byte[] typeArray) {
        try {
            this.messageType = new String(typeArray, CHARSET);
            this.messageTypeArray = typeArray;
        } catch (UnsupportedEncodingException encodingException) {
            PeerProcess.displayLog(encodingException.toString());
        }
    }

    public void setMessageType(String messageType) {
        try {
            this.messageType = messageType.trim();
            this.messageTypeArray = this.messageType.getBytes(CHARSET);
        } catch (UnsupportedEncodingException e) {
            PeerProcess.displayLog(e.toString());
        }
    }

    public void setPayloadArray(byte[] payloadArray) {
        this.payloadArray = payloadArray;
    }

    public String toString() {
        String tempString = null;
        try {
            tempString = String.format("message.Message : Length - [%s], Type - [%s], Content - [%s]",
                    this.messageLength, this.messageType, (new String(this.payloadArray, CHARSET)).trim());
        } catch (UnsupportedEncodingException encodingException) {
            PeerProcess.displayLog(encodingException.toString());
        }
        return tempString;
    }

    public static byte[] messageToByteArray(Message message) {
        byte[] messageByteArray;
        int messageType;

        try {
            messageType = Integer.parseInt(message.getMessageTypeString());

            if (message.getMessageLength().length > MESSAGELength)
                throw new Exception("message.Message Length not valid");
            else if (message.getMessageLength() == null)
                throw new Exception("message.Message Length not valid");
            else if (messageType < 0 || messageType > 7)
                throw new Exception("message.Message Type not valid");
            else if (message.getMessageType() == null)
                throw new Exception("message.Message Type not valid");

            if (message.getPayloadArray()!= null) {
                messageByteArray = new byte[MESSAGELength + MESSAGEType + message.getPayloadArray().length];
                System.arraycopy(message.getMessageLength(), 0, messageByteArray, 0, message.getMessageLength().length);
                System.arraycopy(message.getMessageType(), 0, messageByteArray, MESSAGELength, MESSAGEType);
                System.arraycopy(message.getPayloadArray(), 0, messageByteArray, MESSAGELength + MESSAGEType, message.getPayloadArray().length);
            } else {
                messageByteArray = new byte[MESSAGELength + MESSAGEType];
                System.arraycopy(message.getMessageLength(), 0, messageByteArray, 0, message.getMessageLength().length);
                System.arraycopy(message.getMessageType(), 0, messageByteArray, MESSAGELength, MESSAGEType);
            }
        }
        catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
            messageByteArray = null;
        }

        return messageByteArray;
    }

    public static Message byteArrayToMessage(byte[] byteArray) {
        Message message = new Message();
        byte[] msgLength = new byte[MESSAGELength];
        byte[] msgType = new byte[MESSAGEType];
        byte[] payLoad;
        int messageLength;

        try {
            if (byteArray == null)
                throw new Exception("message.Message not valid");
            else if (byteArray.length < MESSAGELength + MESSAGEType)
                throw new Exception("Length of byte array is not valid");

            System.arraycopy(byteArray, 0, msgLength, 0, MESSAGELength);
            System.arraycopy(byteArray, MESSAGELength, msgType, 0, MESSAGEType);
            message.setMessageLength(msgLength);
            message.setMessageType(msgType);
            messageLength = ConversionUtil.byteArrayToIntegerConverter(msgLength);

            if (messageLength > 1) {
                payLoad = new byte[messageLength-1];
                System.arraycopy(byteArray, MESSAGELength + MESSAGEType,	payLoad, 0, byteArray.length - MESSAGELength - MESSAGEType);
                message.setPayloadArray(payLoad);
            }
        }
        catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
            message = null;
        }
        return message;
    }

}