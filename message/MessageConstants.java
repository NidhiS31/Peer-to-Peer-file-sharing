package message;

public interface MessageConstants {
    String CHARSET = "UTF8";
    int HANDSHAKELength = 32;
    int HEADERLength = 18;
    int ZERO_BITSLength = 10;
    int PEER_IDLength = 4;
    int MESSAGELength = 4;
    int MESSAGEType = 1;
    int PIECELength = 4;
    String HEADERName = "P2PFILESHARINGPROJ";
    String CHOKEPayload = "0";
    String UNCHOKEPayload = "1";
    String INTERESTEDPayload = "2";
    String NOT_INTERESTEDPayload = "3";
    String HAVEPayload = "4";
    String BITFIELDPayload = "5";
    String REQUESTPayload = "6";
    String PIECEPayload = "7";
}