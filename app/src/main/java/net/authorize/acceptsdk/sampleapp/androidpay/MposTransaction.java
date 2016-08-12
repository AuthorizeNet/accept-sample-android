package net.authorize.acceptsdk.sampleapp.androidpay;

public class MposTransaction {

    private ItemInfo itemInfo;
    private static MposTransaction ourInstance = new MposTransaction();

    public static MposTransaction getInstance() {
        return ourInstance;
    }

    private MposTransaction() {
    }

    public ItemInfo getItemInfo() {
        return itemInfo;
    }

    public void setItemInfo(ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
    }
}
