package io.daos.obj;

import io.daos.BufferAllocator;
import io.daos.Constants;
import io.daos.DaosIOException;
import io.daos.DaosTestBase;
import io.netty.buffer.ByteBuf;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DaosObjectIT {

  private static String poolId = DaosTestBase.getPoolId();
  private static String contId = DaosTestBase.getObjectContId();

  private static DaosObjClient client;

  private static AtomicInteger lowSeq = new AtomicInteger();

  private final Random random = new Random();

  @BeforeClass
  public static void beforeClass() throws IOException {
    client = new DaosObjClient.DaosObjClientBuilder().poolId(poolId).containerId(contId).build();
  }

  @Test
  public void testObjectOpen() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
    } finally {
      object.close();
    }
  }

  @Test
  public void testObjectUpdateWithDifferentRecordSize() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
        // update with different record size
        dataSize = 40;
        bytes = generateDataArray(dataSize);
        list.clear();
        list.add(createEntryForUpdate("akey1", 20, 0, dataSize, bytes));
      } finally {
        desc.release();
      }
      desc = object.createDataDescForUpdate("dkey1", list);
      try {
        DaosIOException ee = null;
        try {
          object.update(desc);
        } catch (Exception e) {
          ee = (DaosIOException) e.getCause();
        }
        Assert.assertNotNull(ee);
        Assert.assertTrue(ee instanceof DaosIOException);
        Assert.assertEquals(Constants.ERROR_CODE_ILLEGAL_ARG, ee.getErrorCode());
        // succeed on different key
        dataSize = 40;
        bytes = generateDataArray(dataSize);
        list.clear();
        list.add(createEntryForUpdate("akey2", 20, 0, dataSize, bytes));
      } finally {
        desc.release();
      }
      desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testObjectUpdateWithExistingEntry() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      try {
        // update with different record size
        dataSize = 40;
        bytes = generateDataArray(dataSize);
        // old entry is in list
        list.add(createEntryForUpdate("akey2", 20, 0, dataSize, bytes));
        desc = object.createDataDescForUpdate("dkey1", list);
        Exception ee = null;
        try {
          object.update(desc);
        } catch (Exception e) {
          ee = e;
        }
        Assert.assertNotNull(ee);
        Assert.assertTrue(ee instanceof DaosObjectException);
        Assert.assertTrue(ee.getMessage().contains("failed to update object"));
      } finally {
        desc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testObjectFetchSimple() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      list.add(createEntryForUpdate("akey2", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // fetch akey1
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      IODataDesc.Entry entry = createEntryForFetch("akey1", 10, 0, 80);
      list2.add(entry);
      IODataDesc desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(dataSize, entry.getActualSize());
        byte[] actualBytes = new byte[dataSize];
        ByteBuf buf = entry.getFetchedData();
        buf.readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
      // fetch from offset
      list2.clear();
      entry = createEntryForFetch("akey2", 10, 10, 80);
      list2.add(entry);
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(dataSize - 10, entry.getActualSize());
        byte[] actualBytes2 = new byte[dataSize - 10];
        ByteBuf buf = entry.getFetchedData();
        buf.readBytes(actualBytes2);
        byte[] originBytes = Arrays.copyOfRange(bytes, 10, 30);
        Assert.assertTrue(Arrays.equals(originBytes, actualBytes2));
      } finally {
        desc2.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testUpdateAndFetchSingleType() throws Exception {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 10;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdateWithTypeOfSingle("akey1", 10, 0, dataSize, bytes));
      list.add(createEntryForUpdateWithTypeOfSingle("akey2", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // fetch one akey
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      IODataDesc.Entry entry = createEntryForFetchWithTypeOfSingle("akey2", 10, 0, 10);
      list2.add(entry);
      IODataDesc desc2 = object.createDataDescForFetch("dkey1", list2);
      byte[] actualBytes;
      try {
        object.fetch(desc2);
        Assert.assertEquals(10, entry.getActualSize());
        actualBytes = new byte[entry.getActualSize()];
        ByteBuf buf = entry.getFetchedData();
        buf.readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
      // fetch two akeys
      list2.clear();
      list2.add(createEntryForFetchWithTypeOfSingle("akey2", 10, 0, 10));
      list2.add(createEntryForFetchWithTypeOfSingle("akey1", 10, 0, 10));
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(10, list2.get(0).getActualSize());
        Assert.assertEquals(10, list2.get(1).getActualSize());
        list2.get(0).getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
        list2.get(1).getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testObjectFetchWithIncorrectRecordSize() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      list.add(createEntryForUpdate("akey2", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // fetch akey1, bigger record size
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      IODataDesc.Entry entry = createEntryForFetch("akey1", 20, 0, 25);
      list2.add(entry);
      IODataDesc desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(10, entry.getActualRecSize());
        Assert.assertEquals(20, entry.getActualSize());
        byte[] actualBytes = new byte[entry.getActualSize()];
        entry.getFetchedData().readBytes(actualBytes);
        byte[] bytes020 = Arrays.copyOfRange(bytes, 0, 20);
        Assert.assertTrue(Arrays.equals(bytes020, actualBytes));
      } finally {
        desc2.release();
      }
      // with offset 20
      list2.clear();
      entry = createEntryForFetch("akey1", 20, 20, 10);
      list2.add(entry);
      desc2 = object.createDataDescForFetch("dkey1", list2);
      byte[] actualBytes2;
      try {
        object.fetch(desc2);
        Assert.assertEquals(10, entry.getActualRecSize());
        Assert.assertEquals(10, entry.getActualSize());
        actualBytes2 = new byte[entry.getActualSize()];
        entry.getFetchedData().readBytes(actualBytes2);
        byte[] bytes1020 = Arrays.copyOfRange(bytes, 10, 20);
        Assert.assertTrue(Arrays.equals(bytes1020, actualBytes2));
      } finally {
        desc2.release();
      }
      // fetch akey2, smaller record size, data size < actual record size
      list2.clear();
      entry = createEntryForFetch("akey2", 5, 0, 5);
      list2.add(entry);
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        DaosIOException ee = null;
        try {
          object.fetch(desc2);
        } catch (DaosObjectException e) {
          ee = (DaosIOException) e.getCause();
        }
        Assert.assertNotNull(ee);
        Assert.assertEquals(0, entry.getActualRecSize());
        Assert.assertEquals(Constants.ERROR_CODE_REC2BIG, ee.getErrorCode());
      } finally {
        desc2.release();
      }
      // fetch akey1, smaller record size, data size >= total size
      list2.clear();
      entry = createEntryForFetch("akey1", 5, 0, 30);
      list2.add(entry);
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(10, entry.getActualRecSize());
        Assert.assertEquals(30, entry.getActualSize());
        actualBytes2 = new byte[entry.getActualSize()];
        entry.getFetchedData().readBytes(actualBytes2);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes2));
      } finally {
        desc2.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testObjectFetchSignleWithIncorrectRecordSize() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdateWithTypeOfSingle("akey1", 30, 0, dataSize, bytes));
      list.add(createEntryForUpdateWithTypeOfSingle("akey2", 30, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // fetch akey1, bigger record size
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      IODataDesc.Entry entry = createEntryForFetchWithTypeOfSingle("akey2", 40, 0, 40);
      list2.add(entry);
      IODataDesc desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        Assert.assertEquals(30, entry.getActualRecSize());
        Assert.assertEquals(dataSize, entry.getActualSize());
        byte[] actualBytes = new byte[dataSize];
        entry.getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
      // fetch akey1, smaller record size
      list2.clear();
      entry = createEntryForFetchWithTypeOfSingle("akey1", 20, 0, 20);
      list2.add(entry);
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        DaosIOException ee = null;
        try {
          object.fetch(desc2);
        } catch (DaosObjectException e) {
          ee = (DaosIOException) e.getCause();
        }
        Assert.assertNotNull(ee);
        Assert.assertEquals(Constants.ERROR_CODE_REC2BIG, ee.getErrorCode());
      } finally {
        desc2.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testObjectUpdateAndFetch() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      Assert.assertTrue(object.isOpen());
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      list.add(createEntryForUpdate("akey2", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // fetch akey1
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      list2.add(createEntryForFetch("akey1", 10, 0, 80));
      IODataDesc desc2 = object.createDataDescForFetch("dkey1", list2);
      byte[] actualBytes;
      try {
        object.fetch(desc2);
        IODataDesc.Entry entry = list2.get(0);
        Assert.assertEquals(dataSize, entry.getActualSize());
        actualBytes = new byte[dataSize];
        entry.getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
      // fetch akey2
      list2.clear();
      list2.add(createEntryForFetch("akey2", 10, 0, 30));
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        IODataDesc.Entry entry = list2.get(0);
        Assert.assertEquals(dataSize, entry.getActualSize());
        entry.getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
      // fetch both
      list2.clear();
      list2.add(createEntryForFetch("akey1", 10, 0, 50));
      list2.add(createEntryForFetch("akey2", 10, 0, 60));
      desc2 = object.createDataDescForFetch("dkey1", list2);
      try {
        object.fetch(desc2);
        IODataDesc.Entry entry1 = list2.get(0);
        Assert.assertEquals(dataSize, entry1.getActualSize());
        entry1.getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
        IODataDesc.Entry entry2 = list2.get(1);
        Assert.assertEquals(dataSize, entry2.getActualSize());
        entry2.getFetchedData().readBytes(actualBytes);
        Assert.assertTrue(Arrays.equals(bytes, actualBytes));
      } finally {
        desc2.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testListDkeysSimple() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      list2.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc2 = object.createDataDescForUpdate("dkey2", list2);
      try {
        object.update(desc2);
      } finally {
        desc2.release();
      }
      // list dkeys
      IOKeyDesc keyDesc = object.createKD(null);
      try {
        List<String> keyList = object.listDkeys(keyDesc);
        Assert.assertEquals(2, keyList.size());
        Assert.assertTrue(keyDesc.reachEnd());
      } finally {
        keyDesc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

//  @Test
  public void testPunchAndListDkeys() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      dataSize = 60;
      byte[] bytes2 = generateDataArray(dataSize);
      list2.add(createEntryForUpdate("akey2", 10, 0, dataSize, bytes2));
      IODataDesc desc2 = object.createDataDescForUpdate("dkey2", list2);
      try {
        object.update(desc2);
      } finally {
        desc2.release();
      }
      // list dkeys
      IOKeyDesc keyDesc0 = object.createKD(null);
      try {
        List<String> keyList0 = object.listDkeys(keyDesc0);
        Assert.assertEquals(2, keyList0.size());
        Assert.assertTrue(keyList0.contains("dkey1"));
        Assert.assertTrue(keyList0.contains("dkey2"));
      } finally {
        keyDesc0.release();
      }
      // punch dkey1
      object.punchDkeys(Arrays.asList("dkey1"));
      // list dkey2
      IOKeyDesc keyDesc = object.createKD(null);
      try {
        List<String> keyList1 = object.listDkeys(keyDesc);
        Assert.assertEquals(1, keyList1.size());
        Assert.assertEquals("dkey2", keyList1.get(0));
      } finally {
        keyDesc.release();
      }
      // punch dkey2
      object.punchDkeys(Arrays.asList("dkey2"));
      keyDesc = object.createKD(null);
      try {
        List<String> keyList2 = object.listDkeys(keyDesc);
        Assert.assertEquals(0, keyList2.size());
      } finally {
        keyDesc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  private void listKeysMultipleTimes(String dkey) throws Exception {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 10;
      int nbrOfKeys = (int) (Constants.KEY_LIST_BATCH_SIZE_DEFAULT * 1.5);
      byte[] bytes = generateDataArray(dataSize);
      if (dkey == null) {
        for (int i = 0; i < nbrOfKeys; i++) {
          list.add(createEntryForUpdate("akey" + i, 10, 0, dataSize, bytes));
          IODataDesc desc = object.createDataDescForUpdate("dkey" + i, list);
          try {
            object.update(desc);
          } finally {
            desc.release();
          }
          list.clear();
        }
      } else {
        for (int i = 0; i < nbrOfKeys; i++) {
          list.add(createEntryForUpdate("akey" + i, 10, 0, dataSize, bytes));
        }
        IODataDesc desc = object.createDataDescForUpdate(dkey, list);
        try {
          object.update(desc);
        } finally {
          desc.release();
        }
        list.clear();
      }
      // list keys, reach number limit
      IOKeyDesc keyDesc = object.createKD(dkey);
      try {
        List<String> keys = dkey == null ? object.listDkeys(keyDesc) : object.listAkeys(keyDesc);
        Assert.assertEquals(Constants.KEY_LIST_BATCH_SIZE_DEFAULT, keys.size());
        ByteBuf anchorBuffer = keyDesc.getAnchorBuffer();
        anchorBuffer.readerIndex(0);
        Assert.assertEquals(Constants.KEY_LIST_CODE_REACH_LIMIT, anchorBuffer.readByte());
        String prefix = dkey == null ? "dkey" : "akey";
        assertKeyValue(keys.get(1), prefix, nbrOfKeys);
        assertKeyValue(keys.get(10), prefix, nbrOfKeys);
        assertKeyValue(keys.get(Constants.KEY_LIST_BATCH_SIZE_DEFAULT - 1), prefix, 200);
        // continue to list rest of them
        keyDesc.continueList();
        keys = dkey == null ? object.listDkeys(keyDesc) : object.listAkeys(keyDesc);
        int remaining = nbrOfKeys - Constants.KEY_LIST_BATCH_SIZE_DEFAULT;
        Assert.assertEquals(remaining, keys.size());
        assertKeyValue(keys.get(remaining - 3), prefix, nbrOfKeys);
        assertKeyValue(keys.get(remaining - 2), prefix, nbrOfKeys);
        assertKeyValue(keys.get(remaining - 1), prefix, nbrOfKeys);
        anchorBuffer.readerIndex(0);
        Assert.assertEquals(Constants.KEY_LIST_CODE_ANCHOR_END, anchorBuffer.readByte());
      } finally {
        keyDesc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testListDkeysMultipletimes() throws Exception {
//    listKeysMultipleTimes(null);
  }

  private String varyKey(int idx) {
    StringBuilder sb = new StringBuilder();
    sb.append(idx);
    for (int i = 0; i < idx; i++) {
      sb.append(0);
    }
    return sb.toString();
  }

  private void testListKeysTooBig(String dkey) throws Exception {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 10;
      int nbrOfKeys = 5;
      byte[] bytes = generateDataArray(dataSize);
      if (dkey == null) {
        for (int i = 0; i < nbrOfKeys; i++) {
          list.add(createEntryForUpdate("akey" + i, 10, 0, dataSize, bytes));
          IODataDesc desc = object.createDataDescForUpdate("dkey" + i, list);
          try {
            object.update(desc);
          } finally {
            desc.release();
          }
          list.clear();
        }
      } else {
        for (int i = 0; i < nbrOfKeys; i++) {
          list.add(createEntryForUpdate("akey" + i, 10, 0, dataSize, bytes));
        }
        IODataDesc desc = object.createDataDescForUpdate(dkey, list);
        try {
          object.update(desc);
        } finally {
          desc.release();
        }
        list.clear();
      }
      // list dkeys, small size
      IOKeyDesc keyDesc = object.createKDWithAllParams(dkey, 2, 1, 4);
      try {
        List<String> keys1 = dkey == null ? object.listDkeys(keyDesc) : object.listAkeys(keyDesc);
        Assert.assertEquals(0, keys1.size());
        Assert.assertEquals(6, keyDesc.getSuggestedKeyLen());
        ByteBuf anchorBuffer = keyDesc.getAnchorBuffer();
        anchorBuffer.readerIndex(0);
        Assert.assertEquals(Constants.KEY_LIST_CODE_KEY2BIG, anchorBuffer.readByte());
        // continueList after key2big
        keyDesc.continueList();
        keys1 = dkey == null ? object.listDkeys(keyDesc) : object.listAkeys(keyDesc);
        Assert.assertEquals(2, keys1.size());
        anchorBuffer.readerIndex(0);
        Assert.assertEquals(Constants.KEY_LIST_CODE_REACH_LIMIT, anchorBuffer.readByte());
      } finally {
        keyDesc.release();
      }

      // list 1 key
      IOKeyDesc keyDesc2 = object.createKDWithAllParams(dkey, 1,
        keyDesc.getSuggestedKeyLen(), 1);
      String prefix = dkey == null ? "dkey" : "akey";
      try {
        List<String> keys2 = dkey == null ? object.listDkeys(keyDesc2) : object.listAkeys(keyDesc2);
        Assert.assertEquals(1, keys2.size());
        Assert.assertTrue(keys2.get(0).startsWith(prefix));
        ByteBuf anchorBuffer2 = keyDesc2.getAnchorBuffer();
        anchorBuffer2.readerIndex(0);
        Assert.assertEquals(Constants.KEY_LIST_CODE_REACH_LIMIT, anchorBuffer2.readByte());
      } finally {
        keyDesc2.release();
      }
      // list 3 keys
      IOKeyDesc keyDesc3 = object.createKDWithAllParams(dkey, 1,
        keyDesc.getSuggestedKeyLen(), 1);
      try {
        List<String> keys3;
        ByteBuf anchorBuffer3;
        int size = 0;
        while (!keyDesc3.reachEnd()) {
          keyDesc3.continueList();
          keys3 = dkey == null ? object.listDkeys(keyDesc3) : object.listAkeys(keyDesc3);
          int s1 = keys3.size();
          size += s1;
          anchorBuffer3 = keyDesc3.getAnchorBuffer();
          anchorBuffer3.readerIndex(0);
          if (s1 == 0) {
            if (size < nbrOfKeys) {
              Assert.assertEquals(Constants.KEY_LIST_CODE_KEY2BIG, anchorBuffer3.readByte());
            }
          } else {
            Assert.assertEquals(Constants.KEY_LIST_CODE_REACH_LIMIT, anchorBuffer3.readByte());
            Assert.assertTrue(keys3.get(0).startsWith(prefix));
          }
        }
        Assert.assertEquals(nbrOfKeys, size);
      } finally {
        keyDesc3.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testListDkeysTooBig() throws Exception {
    testListKeysTooBig(null);
  }

  private void assertKeyValue(String key, String prefix, int seq) {
    Assert.assertTrue(key.startsWith(prefix));
    Assert.assertTrue(Integer.valueOf(key.substring(prefix.length())) < seq);
  }

  @Test
  public void testPunchAndListAkeys() throws IOException {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, 30, bytes));
      list.add(createEntryForUpdate("akey2", 10, 0, 30, bytes));
      list.add(createEntryForUpdate("akey3", 10, 0, 30, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      // punch one akey
      object.punchAkeys("dkey1", Arrays.asList("akey1"));
      IOKeyDesc keyDesc = object.createKD("dkey1");
      try {
        List<String> keys = object.listAkeys(keyDesc);
        Assert.assertEquals(2, keys.size());
      } finally {
        keyDesc.release();
      }
      // punch two akeys
      object.punchAkeys("dkey1", Arrays.asList("akey3", "akey2"));
      keyDesc = object.createKD("dkey1");
      try {
        List<String> keys = object.listAkeys(keyDesc);
        Assert.assertEquals(0, keys.size());
      } finally {
        keyDesc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testListAkeysSimple() throws Exception {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 10, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      try {
        object.update(desc);
      } finally {
        desc.release();
      }
      List<IODataDesc.Entry> list2 = new ArrayList<>();
      list2.add(createEntryForUpdate("akey2", 10, 0, dataSize, bytes));
      IODataDesc desc2 = object.createDataDescForUpdate("dkey1", list2);
      try {
        object.update(desc2);
      } finally {
        desc2.release();
      }
      // list akeys from non-existing dkey
      IOKeyDesc keyDesc = object.createKD("dkey2");
      try {
        List<String> keys = object.listAkeys(keyDesc);
        Assert.assertEquals(0, keys.size());
        Assert.assertTrue(keyDesc.reachEnd());
      } finally {
        keyDesc.release();
      }
      // list akeys
      keyDesc = object.createKD("dkey1");
      try {
        List<String> keys = object.listAkeys(keyDesc);
        Assert.assertEquals(2, keys.size());
        Assert.assertTrue(keyDesc.reachEnd());
      } finally {
        keyDesc.release();
      }
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testListAkeysMultipletimes() throws Exception {
//    listKeysMultipleTimes("dkey2");
  }

  @Test
  public void testListAkeysTooBig() throws Exception {
    testListKeysTooBig("dkey3");
  }

  private IODataDesc.Entry createEntryForFetch(String akey, IODataDesc.IodType type, int recordSize,
                                               int offset, int dataSize) throws IOException {
    return IODataDesc.createEntryForFetch(akey, type, recordSize, offset, dataSize);
  }

  private IODataDesc.Entry createEntryForFetch(String akey, int recordSize, int offset, int dataSize)
    throws IOException {
    return createEntryForFetch(akey, IODataDesc.IodType.ARRAY, recordSize, offset, dataSize);
  }

  private IODataDesc.Entry createEntryForFetchWithTypeOfSingle(String akey, int recordSize, int offset, int dataSize)
    throws IOException {
    return createEntryForFetch(akey, IODataDesc.IodType.SINGLE, recordSize, offset, dataSize);
  }

  private IODataDesc.Entry createEntryForUpdate(String akey, IODataDesc.IodType type, int recordSize, int offset,
                                                int dataSize, byte[] bytes) throws IOException {
    ByteBuf buffer = BufferAllocator.objBufWithNativeOrder(dataSize);
    buffer.writeBytes(bytes);
    return IODataDesc.createEntryForUpdate(akey, type, recordSize, offset, buffer);
  }

  private IODataDesc.Entry createEntryForUpdateWithTypeOfSingle(String akey, int recordSize, int offset, int dataSize,
                                                byte[] bytes) throws IOException {
    return createEntryForUpdate(akey, IODataDesc.IodType.SINGLE, recordSize, offset, dataSize, bytes);
  }

  private IODataDesc.Entry createEntryForUpdate(String akey, int recordSize, int offset, int dataSize,
                                                byte[] bytes) throws IOException {
    return createEntryForUpdate(akey, IODataDesc.IodType.ARRAY, recordSize, offset, dataSize, bytes);
  }

  private byte[] generateDataArray(int dataSize) {
    byte[] bytes = new byte[dataSize];
    for (int i = 0; i < dataSize; i++) {
      bytes[i] = (byte) ((i + 33) % 128);
    }
    return bytes;
  }

  @Test
  public void testGetRecordSize() throws Exception {
    DaosObjectId id = new DaosObjectId(random.nextInt(), lowSeq.incrementAndGet());
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 30;
      byte[] bytes = generateDataArray(dataSize);
      list.add(createEntryForUpdate("akey1", 1, 0, dataSize, bytes));
      IODataDesc desc = object.createDataDescForUpdate("dkey1", list);
      object.update(desc);
      Assert.assertEquals(1, object.getRecordSize("dkey1", "akey1"));
      Assert.assertEquals(0, object.getRecordSize("dkey1", "akey2"));
      Assert.assertEquals(0, object.getRecordSize("dkey2", "akey2"));
    } finally {
      if (object.isOpen()) {
        object.punch();
      }
      object.close();
    }
  }

  @Test
  public void testReadData() throws Exception {
    DaosObjectId id = new DaosObjectId(202011021441510000L, 12);
    id.encode();
    DaosObject object = client.getObject(id);
    try {
      object.open();
      List<IODataDesc.Entry> list = new ArrayList<>();
      int dataSize = 8556;
      list.add(createEntryForFetch("5301", 1, 0, dataSize));
      IODataDesc desc = object.createDataDescForUpdate("27", list);
      object.update(desc);
      Assert.assertEquals(dataSize, desc.getEntry(0).getActualSize());
    } finally {
//      if (object.isOpen()) {
//        object.punch();
//      }
      object.close();
    }
  }

  @AfterClass
  public static void afterClass() throws IOException {
    if (client != null) {
      client.close();
    }
  }
}
