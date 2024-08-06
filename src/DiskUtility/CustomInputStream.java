package DiskUtility;
import Constants.DATA_STORE_BLOCK_FRAME;
import FileSystem.INode;
import FileSystem.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class CustomInputStream extends InputStream{
    private Gateway gateway;
    private byte[] buffer;
    private long pointer;
    private int bufferPointer;
    private long length;
    private Gateway.NodeEntry nodeEntry;
    private int currentExtent; // Index of the current extentFrame within the LinkedList of extentFrames.
    private long runningExtentPointer;
    private boolean isThumbnail;
    private DataStoreGateway dataStore;
    public CustomInputStream(Gateway gateway, INode iNode, boolean isThumbnail) throws Exception{
        this.gateway = gateway;
        this.buffer = new byte[DATA_STORE_BLOCK_FRAME.FULL_SIZE];
        try{
            this.nodeEntry = gateway.__getINodeDetails(iNode);
        } catch (Exception e){
            System.out.println("Unable to create InputStream. Unable to get NodeEntry. " + e.getMessage());
        }
        this.length = this.nodeEntry.iNode.getiNodeSize();
        this.currentExtent = 0;
        this.pointer = 0;
        this.bufferPointer = 0;
        this.runningExtentPointer = 0;
        this.isThumbnail = isThumbnail;
        if (isThumbnail){
            dataStore = gateway.getThumbnailStoreGateway();
        } else {
            dataStore = gateway.getDataStoreGateway();
        }
    }
    public CustomInputStream(Gateway gateway, INode iNode) throws Exception{
        this(gateway, iNode, false);
    }

    @Override
    public int read() throws IOException {
        int value;
        if (pointer == length){
            return -1;
        }
        // Populate the buffer. Get the next byte from the buffer.
        // Repopulate the buffer when at the end of the buffer.
        if (pointer == 0 || bufferPointer == buffer.length) {
            try {
                __populateBuffer();
            } catch (Exception e){
                throw new IOException("Unable to get bytes from the dataBlock: " + e.getMessage());
            }
        }
        value = buffer[bufferPointer] & 0xFF; // BIT AND to ensure that value is between 0 and 255
        __incrementPointers();
        return value;
    }

    /**
     * This method populates/repopulates the buffer (grabs the next bytes starting from the pointer index)
     * Sets the bufferPointer to 0.
     */
    private void __populateBuffer() throws Exception{
        ExtentStoreGateway.ExtentFrame extentFrame = nodeEntry.extentFrames.get(currentExtent);
        // Number of bytes populated at a given time. If the bytesPopulated is not the same as `buffer.length` and all
        // the bytes of an extent have been read, get another extent. Continue the loop till
        // `bytesPopulated` == `buffer.length`;
        int bytesPopulated = 0;
        while ((bytesPopulated + pointer) < length && bytesPopulated != buffer.length){
            // Check if there are any bytes left in the current extentFrame. If not, get the next extentFrame.
            if (runningExtentPointer == extentFrame.length){
                // The data in the runningExtent has run out. Get a new Extent
                currentExtent++;
                extentFrame = nodeEntry.extentFrames.get(currentExtent);
                runningExtentPointer = 0;
            }
            int bytesRead = 0;
            bytesRead =  dataStore.populateBufferFromExtent(buffer, nodeEntry.extentFrames.get(currentExtent), bytesPopulated ,runningExtentPointer);
            bytesPopulated += bytesRead;
            runningExtentPointer += bytesRead;
        }
        bufferPointer = 0;
    }
    private void __incrementPointers(){
        pointer++;
        bufferPointer++;
    }
}
