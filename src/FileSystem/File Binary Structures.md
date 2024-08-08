### Binary Structures of Files
    ALL FRAMES HAVE ENCRYPTION INFORMATION BUT IT IS ONLY SHOWN IN THE SUPERBLOCK FRAME.
    MAGIC_VALUE                 -    4 bytes (0x61717561)
    The Preset Filenames are encoded in ASCII.
    User filenames are encoded in UTF-8

#### SuperBlock FULL FRAME
    IV Value                    -       12 bytes                                || Starting Index: 0
    Magic Value                 -       4 bytes                                 || Starting Index: 12
    Flags                       -       1 byte                                  || Starting Index: 16
    FileSystem Name (UTF-8)     -       256 bytes                               || Starting Index: 17
    Magic Value                 -       4 bytes                                 || Starting Index: 273
    Total DirectoryStores       -       8 bytes                                 || Starting Index: 277
    Total DataStores            -       8 bytes                                 || Starting Index: 285     
    Total ThumbnailStores       -       8 bytes                                 || Starting Index: 293
    Total INodeStores           -       8 bytes                                 || Starting Index: 301
    Total ExtentStores          -       8 bytes                                 || Starting Index: 309
    Total AttributeStores       -       8 bytes                                 || Starting Index: 317
    SALT Value                  -       16 bytes                                || Starting Index: 325
    Magic Value                 -       4 bytes                                 || Starting Index: 341
    TAG                         -       16 bytes                                || Starting Index: 345
    
    Size: 361 Bytes
    BASE FRAME does not include the IV and TAG. Size of BASE FRAME = 333
    
#### INode Entry
    md5 checksum                -       16 bytes                                || Starting Index: 0
    iNodeAddress                -       8 bytes                                 || Starting Index: 16
    size                        -       8 bytes                                 || Starting Index: 24
    flags                       -       1 byte                                  || Starting Index: 32
    creationTime                -       8 bytes                                 || Starting Index: 33
    lastModifiedTime            -       8 bytes                                 || Starting Index: 41
    extentStoreAddress          -       8 bytes                                 || Starting Index: 49
    extentCount                 -       8 bytes                                 || Starting Index: 57
    thumbnailStoreAddress       -       8 bytes                                 || Starting Index: 65
    Size: 73 bytes



#### DirectoryStore Entry
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    index                       -       8 bytes                                 || Starting Index: 4
    parentIndex                 -       8 bytes                                 || Starting Index: 12
    previousSiblingIndex        -       8 bytes                                 || Starting Index: 20
    nextSiblingIndex            -       8 bytes                                 || Starting Index: 28
    childIndex                  -       8 bytes                                 || Starting Index: 36
    iNode                       -       8 bytes                                 || Starting Index: 44
    parentINode                 -       8 bytes                                 || Starting Index: 52       // DELETE IN FUTURE AS PARENT CANNOT HAVE INODE ADDRESS.
    flags                       -       1 byte                                  || Starting Index: 60
    Magic Value                 -       4 bytes                                 || Starting Index: 61
    name                        -       256 bytes                               || Starting Index: 65
    Magic Value                 -       4 bytes                                 || Starting Index: 321

    Size: 325 bytes

#### ExtentStore Entry
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    DataStore Index             -       8 bytes                                 || Starting Index: 4
    DataStore Offset            -       4 bytes                                 || Starting Index: 12
    Length                      -       8 bytes                                 || Starting Index: 16 
    Next Extent Entry Address   -       8 bytes                                 || Starting Index: 24
    Size: 32 Bytes

#### Modified ExtentStore Entry (In case of variable extent entries: not placed consecutively)
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    Extent Address              -       8 bytes                                 || Starting Index: 4
    DataStore Index             -       8 bytes                                 || Starting Index: 12
    DataStore Offset            -       4 bytes                                 || Starting Index: 20
    Length                      -       8 bytes                                 || Starting Index: 24
    Next Extent Frame           -       8 bytes                                 || Starting Index: 32

    Size: 40 bytes
    

#### DataStore Block
    IV Value                    -      12 bytes                                 
    MD5 hash                    -      16 bytes                                 || Starting Index: 0
    Bytes Occupied              -       2 bytes                                 || Starting Index: 16
    Block Bitmap                -       450 bytes                               || Starting Index: 18
    Data Bytes                  -       3600 bytes                              || Starting Index: 468
    Tag                         -       16 bytes
    BASE SIZE: 4068 bytes
    FULL SIZE: 4096 bytes

#### DataStore Entry
    Just plain data is pasted.

#### ThumbnailStore Header
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    ThumbnailStore Number       -       4 bytes                                 || Starting Index: 4
    flags                       -       1 byte                                  || Starting Index: 8
    
    Size: 9 bytes

#### ThumbnailStore Entry
    TO BE WRITTEN

### FLAGS

#### NODE
    RUNTIME_FLAGS
    _____________
    CNR     -       Children Not Read : Set to true if children of the Node have not been read from disk.