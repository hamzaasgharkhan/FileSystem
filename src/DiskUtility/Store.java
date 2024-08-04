package DiskUtility;

public enum Store {
    DirectoryStore("directory-store", BitmapType.Singular),
    INodeStore("inode-store", BitmapType.Singular),
    ExtentStore("extent-store", BitmapType.Singular),
    DataStore("data-store", BitmapType.Half),
    ThumbnailStore("thumbnail-store", BitmapType.Half);

    public final String fileName;
    public final BitmapType bitmapType;
    Store(String fileName, BitmapType bitmapType){
        this.fileName = fileName;
        this.bitmapType = bitmapType;
    }
}

enum BitmapType{
    Singular,
    Half
}