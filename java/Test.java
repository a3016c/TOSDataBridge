/*
Copyright (C) 2017 Jonathon Ogden   <jeog.dev@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses.
*/

import com.github.jeog.tosdatabridge.TOSDataBridge;
import com.github.jeog.tosdatabridge.TOSDataBridge.*;
import com.github.jeog.tosdatabridge.DataBlock;
import com.github.jeog.tosdatabridge.DataBlock.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Test{    

    private static final int SLEEP_PERIOD = 1000;

    public static void
    main(String[] args)
    {
        if(args.length == 0){
            System.err.println("TOSDataBridge library path must be passed as arg0");
            return;
        }

        if( !(new File(args[0])).exists() ){
            System.err.println("Arg0 (" + args[0] + ") is not a valid file.");
            throw new IllegalArgumentException("Arg0 is not a valid file");
        }

        if( !TOSDataBridge.init(args[0]) ){
            System.err.println("Failed to load library (" + args[0] + ")");
            throw new RuntimeException("failed to load library");
        }

        try{
            testConnection();
            testAdminCalls();

            int block1Sz = 10;
            boolean block1DateTime = true;
            int block1Timeout = 3000;

            System.out.println();
            System.out.println("CREATE BLOCK...");
            DataBlock block1 = new DataBlock(block1Sz, block1DateTime, block1Timeout);

            if( !testBlockState(block1, block1Sz, block1DateTime, block1Timeout) )
                return;
            else
                System.out.println("Successfully created block: " + block1.getName());

            System.out.println();
            System.out.println("Double block size...");
            block1.setBlockSize(block1.getBlockSize() * 2);
            if(block1.getBlockSize() != 2 * block1Sz){
                System.err.println("failed to double block size");
                return;
            }

            String item1 = "SPY";
            String item2 = "QQQ";
            String topic1 = "LAST"; // double
            String topic2 = "VOLUME"; // long
            String topic3 = "LASTX"; // string

            System.out.println("Add item: " + item1);
            block1.addItem(item1);
            printBlockItemsTopics(block1);

            System.out.println("Add item: " + item2);
            block1.addItem(item2);
            printBlockItemsTopics(block1);

            System.out.println("Add topic: " + topic1);
            block1.addTopic(topic1);
            printBlockItemsTopics(block1);

            System.out.println("Remove item: " + item1);
            block1.removeItem(item1);
            printBlockItemsTopics(block1);

            System.out.println("Remove Topic: " + topic1);
            block1.removeTopic(topic1);
            printBlockItemsTopics(block1);

            System.out.println("Add ALL items");
            block1.addItem(item1);
            block1.addItem(item2);
            printBlockItemsTopics(block1);

            System.out.println("Add ALL topics");
            block1.addTopic(topic1);
            block1.addTopic(topic2);
            block1.addTopic(topic3);
            printBlockItemsTopics(block1);

            System.out.println("***SLEEP FOR " + String.valueOf(SLEEP_PERIOD*3) + " MILLISECONDS***");
            Thread.sleep(SLEEP_PERIOD*3);
            System.out.println();

            System.out.println("TEST GET CALLS, BLOCK: " + block1.getName());
            testGetCalls(block1,false);
            System.out.println();

            System.out.println("TEST GET CALLS (WITH DATETIME), BLOCK: " + block1.getName());
            testGetCalls(block1,true);
            System.out.println();

            System.out.println("TEST STREAM SNAPSHOT CALLS, BLOCK: " + block1.getName());
            testStreamSnapshotCalls(block1,5,false);
            System.out.println();

            System.out.println("TEST STREAM SNAPSHOT CALLS (WITH DATETIME), BLOCK: " + block1.getName());
            testStreamSnapshotCalls(block1,3,true);
            System.out.println();

            System.out.println();
            System.out.println("***SLEEP FOR " + String.valueOf(SLEEP_PERIOD) + " MILLISECONDS***");
            Thread.sleep(SLEEP_PERIOD);
            System.out.println();

            System.out.println("TEST STREAM SNAPSHOT FROM MARKER CALLS, BLOCK: " + block1.getName());
            testStreamSnapshotFromMarkerCalls(block1,3,SLEEP_PERIOD,false,false);
            System.out.println();

            System.out.println("TEST STREAM SNAPSHOT FROM MARKER (WITH DATETIME) CALLS, BLOCK: " + block1.getName());
            testStreamSnapshotFromMarkerCalls(block1,3,SLEEP_PERIOD,true,true);
            System.out.println();

            System.out.println("TEST TOTAL FRAME CALLS: " + block1.getName());
            testTotalFrameCalls(block1, false);
            System.out.println();

            System.out.println("TEST TOTAL FRAME (WITH DATETIME) CALLS, BLOCK: " + block1.getName());
            testTotalFrameCalls(block1, true);
            System.out.println();

            System.out.println("TEST ITEM FRAME CALLS: " + block1.getName());
            testItemFrameCalls(block1, false);
            System.out.println();

            System.out.println("TEST ITEM FRAME (WITH DATETIME) CALLS, BLOCK: " + block1.getName());
            testItemFrameCalls(block1, true);
            System.out.println();

            System.out.println("TEST TOPIC FRAME CALLS: " + block1.getName());
            testTopicFrameCalls(block1, false);
            System.out.println();

            System.out.println("TEST TOPIC FRAME (WITH DATETIME) CALLS, BLOCK: " + block1.getName());
            testTopicFrameCalls(block1, true);
            System.out.println();


        }catch(LibraryNotLoaded e){
            System.out.println("EXCEPTION: LibraryNotLoaded");
            System.out.println(e.toString());
            e.printStackTrace();
        }catch(CLibException e){
            System.out.println("EXCEPTION: CLibException");
            System.out.println(e.toString());
            e.printStackTrace();
        } catch (DateTimeNotSupported e) {
            System.out.println("EXCEPTION: DateTimeNotSupported");
            System.out.println(e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("EXCEPTION: InterruptedException");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    private static void
    testConnection() throws LibraryNotLoaded
    {
        // connect() (should already be connected)
        TOSDataBridge.connect();

        // connected()
        if(!TOSDataBridge.connected()){
            System.out.println("NOT CONNECTED");
            return;
        }

        // connection_state()
        int connState = TOSDataBridge.connectionState();
        System.out.println("CONNECTION STATE: " + String.valueOf(connState));
        if(connState != TOSDataBridge.CONN_ENGINE_TOS){
            System.out.println("INVALID CONNECTION STATE");
            return;
        }
    }

    private static boolean
    testAdminCalls() throws LibraryNotLoaded, CLibException
    {
        // get_block_limit()
        int l = TOSDataBridge.getBlockLimit();
        System.out.println("block limit: " + String.valueOf(l));

        // set_block_limit()
        System.out.println("double block limit");
        TOSDataBridge.setBlockLimit(l*2);

        // get_block_limit()
        int ll = TOSDataBridge.getBlockLimit();
        if( ll != l * 2 ){
            System.err.println("failed to double block limit");
            return false;
        }
        System.out.println("block limit: " + String.valueOf(l));

        // get_block_count()
        int c = TOSDataBridge.getBlockCount();
        if(c != 0){
            System.err.println("initial block count != 0");
            return false;
        }
        System.out.println("block count: " + String.valueOf(c));

        // type_bits()
        if(!testTypeBits("LAST", TOSDataBridge.QUAD_BIT, "QUAD_BIT",
                         TOSDataBridge.TOPIC_IS_DOUBLE, "TOPIC_IS_DOUBLE")) {
            return false;
        }

        if(!testTypeBits("EPS", 0, "", TOSDataBridge.TOPIC_IS_DOUBLE, "TOPIC_IS_DOUBLE"))
            return false;

        if(!testTypeBits("VOLUME", TOSDataBridge.QUAD_BIT | TOSDataBridge.INTGR_BIT, "QUAD_BIT | INTGR_BIT",
                         TOSDataBridge.TOPIC_IS_LONG, "TOPIC_IS_LONG")){
            return false;
        }

        if(!testTypeBits("LAST_SIZE", TOSDataBridge.INTGR_BIT, "INTGR_BIT",
                         TOSDataBridge.TOPIC_IS_LONG, "TOPIC_IS_LONG")){
            return false;
        }

        if(!testTypeBits("SYMBOL", TOSDataBridge.STRING_BIT, "STRING_BIT",
                         TOSDataBridge.TOPIC_IS_STRING, "TOPIC_IS_STRING")) {
            return false;
        }

        return true;
    }

    private static boolean
    testTypeBits(String topic, int cBits, String cName, int jTypeId, String jName)
            throws CLibException, LibraryNotLoaded
    {
        if( TOSDataBridge.getTypeBits(topic) != cBits) {
            System.err.println("Type Bits for '" + topic + "' != " + cName + "(" + String.valueOf(cBits) + ")");
            return false;
        }

        if( TOSDataBridge.getTopicType(topic) != jTypeId) {
            System.err.println("Topic type for '" + topic + "' != " + jName + "(" + String.valueOf(jTypeId) + ")");
            return false;
        }

        return true;
    }

    private static boolean
    testBlockState(DataBlock block, int blockSize, boolean withDateTime, int timeout)
            throws CLibException, LibraryNotLoaded
    {
        if(block.getBlockSize() != blockSize){
            System.out.println("invalid block size: "
                    + String.valueOf(block.getBlockSize()) + ", "
                    + String.valueOf(blockSize));
            return false;
        }

        if(block.isUsingDateTime() != withDateTime){
            System.out.println("invalid block DateTime: "
                    + String.valueOf(block.isUsingDateTime()) + ", "
                    + String.valueOf(withDateTime));
            return false;
        }

        if(block.getTimeout() != timeout){
            System.out.println("invalid block timeout: "
                    + String.valueOf(block.getTimeout()) + ", "
                    + String.valueOf(timeout));
            return false;
        }

        System.out.println("Block: " + block.getName());
        System.out.println("using datetime: " + String.valueOf(block.isUsingDateTime()));
        System.out.println("timeout: " + String.valueOf(block.getTimeout()));
        System.out.println("block size: " + String.valueOf(block.getBlockSize()));
        return true;
    }

    private static void
    testGetCalls(DataBlock block, boolean withDateTime)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        Random rand = new Random(Double.doubleToLongBits(Math.random()));
        String dtSuffix = withDateTime ? "WithDateTime" : "";

        Set<String> items = block.getItems();
        Set<String> topics = block.getTopics();

        for(String topic : topics){
            int tType = TOSDataBridge.getTopicType(topic);

            for(String item : items) {
                int occ = block.getStreamOccupancy(item,topic);

                for(int i : new int[]{0, 1 + rand.nextInt(occ-1), occ-1}) {
                    switch (tType) {
                        case TOSDataBridge.TOPIC_IS_LONG:
                            printGet(block, item, topic, i, "getLong" + dtSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_DOUBLE:
                            printGet(block, item, topic, i, "getDouble" + dtSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_STRING:
                            printGet(block, item, topic, i, "getString" + dtSuffix);
                            break;
                    }
                }
            }
        }

    }

    private static void
    testStreamSnapshotCalls(DataBlock block, int sz, boolean withDateTime)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        String dtSuffix = withDateTime ? "WithDateTime" : "";

        Set<String> items = block.getItems();
        Set<String> topics = block.getTopics();

        if(sz < 1)
            throw new IllegalArgumentException("sz < 1");

        for(String topic : topics){
            int tType = TOSDataBridge.getTopicType(topic);

            for(String item : items) {
                int occ = block.getStreamOccupancy(item,topic);

                for(int[] ii : new int[][]{{0,sz-1}, {occ-sz,-1}}) {
                    switch (tType) {
                        case TOSDataBridge.TOPIC_IS_LONG:
                            printGetStreamSnapshot(block,item,topic,ii[1],ii[0],"getStreamSnapshotLongs" + dtSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_DOUBLE:
                            printGetStreamSnapshot(block,item,topic,ii[1],ii[0],"getStreamSnapshotDoubles" + dtSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_STRING:
                            printGetStreamSnapshot(block,item,topic,ii[1],ii[0],"getStreamSnapshotStrings" + dtSuffix);
                            break;
                    }
                }
            }
        }

    }

    private static void
    testStreamSnapshotFromMarkerCalls(DataBlock block, int passes, int wait, boolean withDateTime,
                                      boolean ignoreDirty)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        String callSuffix = withDateTime ? "WithDateTime" : "";
        callSuffix = callSuffix + (ignoreDirty ? "IgnoreDirty" : "");

        Set<String> items = block.getItems();
        Set<String> topics = block.getTopics();

        if(passes < 1)
            throw new IllegalArgumentException("passes < 1");

        for(String topic : topics){
            int tType = TOSDataBridge.getTopicType(topic);

            for(String item : items) {
                int occ = block.getStreamOccupancy(item,topic);

                for(int i = 0; i < passes; ++i) {
                    System.out.print("PASS #" + String.valueOf(i+1) + " :: ");
                    switch (tType) {
                        case TOSDataBridge.TOPIC_IS_LONG:
                            printGetStreamSnapshotFromMarker(
                                    block,item,topic,0,"getStreamSnapshotLongsFromMarker" + callSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_DOUBLE:
                            printGetStreamSnapshotFromMarker(
                                    block,item,topic,0,"getStreamSnapshotDoublesFromMarker" + callSuffix);
                            break;
                        case TOSDataBridge.TOPIC_IS_STRING:
                            printGetStreamSnapshotFromMarker(
                                    block,item,topic,0,"getStreamSnapshotStringsFromMarker" + callSuffix);
                            break;
                    }
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static <T> void
    testTotalFrameCalls(DataBlock block, boolean withDateTime)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        Map<String,Map<String,T>> frame =
            (Map<String,Map<String,T>> )(withDateTime ? block.getTotalFrameWithDateTime()
                                                      : block.getTotalFrame());

        for(String i : frame.keySet()){
            System.out.print(String.format("%-12s :::  ", i));
            Map<String,T> row = frame.get(i);
            for(String t : row.keySet()){
                if(withDateTime){
                    DateTimePair<String> p = (DateTimePair<String>)row.get(t);
                    System.out.print(String.format("%s %s %s  ", t, p.first, p.second.toString()));
                }else {
                    System.out.print(String.format("%s %s  ", t, row.get(t)));
                }
            }
            System.out.println();
        }
    }

    private static <T> void
    testItemFrameCalls(DataBlock block,  boolean withDateTime)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        Set<String> topics = block.getTopics();

        for(String topic : topics) {
            Map<String, T> frame =
                (Map<String, T>) (withDateTime ? block.getItemFrameWithDateTime(topic)
                                               : block.getItemFrame(topic));

            System.out.print(String.format("%-12s :::  ", topic));
            for(String item : frame.keySet()){
                if(withDateTime){
                    DateTimePair<String> p = (DateTimePair<String>)frame.get(item);
                    System.out.print(String.format("%s %s %s  ", item, p.first, p.second.toString()));
                }else {
                    System.out.print(String.format("%s %s  ", item, frame.get(item)));
                }
            }
            System.out.println();
        }
    }

    private static <T> void
    testTopicFrameCalls(DataBlock block,  boolean withDateTime)
            throws CLibException, LibraryNotLoaded, DateTimeNotSupported
    {
        Set<String> items = block.getItems();

        for(String item : items) {
            Map<String, T> frame =
                (Map<String, T>)(withDateTime ? block.getTopicFrameWithDateTime(item)
                                              : block.getTopicFrame(item));

            System.out.print(String.format("%-12s :::  ", item));
            for(String topic: frame.keySet()){
                if(withDateTime){
                    DateTimePair<String> p = (DateTimePair<String>)frame.get(topic);
                    System.out.print(String.format("%s %s %s  ", topic, p.first, p.second.toString()));
                }else {
                    System.out.print(String.format("%s %s  ", topic, frame.get(topic)));
                }
            }
            System.out.println();
        }
    }

        private static <T> void
    printGet(DataBlock block, String item, String topic, int indx, String mname)
            throws CLibException, LibraryNotLoaded,
                   DateTimeNotSupported
    {
        Method m;
        try {
            m = block.getClass().getMethod(mname,String.class,String.class,int.class,boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        try {
            T r1 = (T)m.invoke(block,item,topic,indx,true);
            System.out.println(mname + "(" + item + "," + topic + "," + String.valueOf(indx)
                                     + "): " + r1.toString());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static <T> void
    printGetStreamSnapshot(DataBlock block, String item,
                           String topic, int end, int beg, String mname)
            throws CLibException, LibraryNotLoaded,
            DateTimeNotSupported
    {
        Method m;
        try {
            m = block.getClass().getMethod(mname,String.class,String.class,int.class,int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        try {
            T[] r1 = (T[])m.invoke(block,item,topic,end,beg);
            System.out.println(mname + "(" + item + "," + topic + "," + String.valueOf(beg)
                               + " to " + String.valueOf(end) + "): ");
            for(int i = r1.length-1; i >= 0; --i){
                 System.out.println(String.valueOf(i) + ": " + r1[i].toString());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static <T> void
    printGetStreamSnapshotFromMarker(DataBlock block, String item,
                                     String topic, int beg, String mname)
            throws CLibException, LibraryNotLoaded,
            DateTimeNotSupported
    {
        Method m;
        try {
            m = block.getClass().getMethod(mname,String.class,String.class,int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        try {
            T[] r1 = (T[])m.invoke(block,item,topic,beg);
            System.out.println(mname + "(" + item + "," + topic + "," + String.valueOf(beg) + "): ");
            for(int i = r1.length-1; i >= 0; --i){
                System.out.println(String.valueOf(i) + ": " + r1[i].toString());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static void
    printBlockItemsTopics(DataBlock block)
            throws CLibException, LibraryNotLoaded
    {
        System.out.println("Block: " + block.getName());
        for(String item : block.getItems())
            System.out.println("item: " + item);
        for(String topic : block.getTopics())
            System.out.println("topic: " + topic);
        for(String item : block.getItemsPreCached())
            System.out.println("item(pre-cache): " + item);
        for(String topic : block.getTopicsPreCached())
            System.out.println("topic(pre-cache): " + topic);
        System.out.println();
    }

}