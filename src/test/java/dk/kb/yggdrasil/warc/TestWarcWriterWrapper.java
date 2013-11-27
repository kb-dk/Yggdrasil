package dk.kb.yggdrasil.warc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.common.Base32;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class TestWarcWriterWrapper {

	public String getUrlPath(URL url) {
		String path = url.getFile();
		path = path.replaceAll( "%5b", "[" );
		path = path.replaceAll( "%5d", "]" );
		return path;
	}

	@Test
	public void test_warcwriterwrapper() {
		URL url;
		File file;
		ByteArrayInputStream in;
		ByteCountingPushBackInputStream pbin;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] tmpBuf = new byte[1024];
		int read;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] digestBytes;
			ContentType contentType;
			WarcDigest blockDigest;

			// Get eclipse/maven target test folder.
			url = this.getClass().getClassLoader().getResource( "" );
			file = new File(getUrlPath(url));

			File warcFile = new File(file, "42");
			if (warcFile.exists()) {
				if (!warcFile.delete()) {
					Assert.fail("Unable to remove data from previous run!");
				}
			}

			WarcWriterWrapper w3 = WarcWriterWrapper.getWriter(file, "42");
			Assert.assertNotNull(w3);
			Assert.assertTrue(w3.bIsNew);

			String warcFields = "greetings: hi mom!\n";
            byte[] warcFieldsBytes = warcFields.getBytes("UTF-8");
			md.reset();
			digestBytes = md.digest(warcFieldsBytes);
			blockDigest = WarcDigest.createWarcDigest("SHA1", digestBytes, "Base32", Base32.encodeArray(digestBytes));
			Uri warcinfoId = w3.writeWarcinfoRecord(warcFieldsBytes, blockDigest);

			Assert.assertNotNull(warcinfoId);

			String dataStr = "very interesting data!";
			byte[] dataBytes = dataStr.getBytes("UTF-8");
			md.reset();
			digestBytes = md.digest(dataBytes);
			in = new ByteArrayInputStream(dataBytes);
			contentType = ContentType.parseContentType("application/binary");
			blockDigest = WarcDigest.createWarcDigest("SHA1", digestBytes, "Base32", Base32.encodeArray(digestBytes));
			Uri warcResourceId = w3.writeResourceRecord(in, dataBytes.length, contentType, blockDigest);

			Assert.assertNotNull(warcResourceId);

			String metadataStr = "very interesting metadata!";
			byte[] metadataBytes = metadataStr.getBytes("UTF-8");
			md.reset();
			digestBytes = md.digest(metadataBytes);
			in = new ByteArrayInputStream(metadataBytes);
			contentType = ContentType.parseContentType("text/xml; charset=\"utf-8\"");
			blockDigest = WarcDigest.createWarcDigest("SHA1", digestBytes, "Base32", Base32.encodeArray(digestBytes));
			Uri warcMetadataId = w3.writeMetadataRecord(in, metadataBytes.length, contentType, warcResourceId, blockDigest);

			Assert.assertNotNull(warcMetadataId);

			w3.close();

			RandomAccessFile raf = new RandomAccessFile(new File(file, "42"), "r");
			RandomAccessFileInputStream rafin = new RandomAccessFileInputStream(raf);
			WarcReader reader = WarcReaderFactory.getReader(rafin, 8192);
			WarcRecord record;
			WarcHeader header;

			record = reader.getNextRecord();
			Assert.assertNotNull(record);
			Assert.assertTrue(record.isCompliant());
			header = record.header;
			Assert.assertEquals(new Integer(WarcConstants.RT_IDX_WARCINFO), header.warcTypeIdx);
			Assert.assertNull(header.warcWarcinfoIdUri);
			Assert.assertEquals(warcinfoId, header.warcRecordIdUri);
			Assert.assertNull(header.warcRefersToUri);

			pbin = record.getPayload().getInputStream();
			out.reset();
			while ((read = pbin.read(tmpBuf)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			out.close();
			pbin.close();
			Assert.assertArrayEquals(warcFieldsBytes, out.toByteArray());

			record = reader.getNextRecord();
			Assert.assertNotNull(record);
			Assert.assertTrue(record.isCompliant());
			header = record.header;
			Assert.assertEquals(new Integer(WarcConstants.RT_IDX_RESOURCE), header.warcTypeIdx);
			Assert.assertEquals(warcinfoId, header.warcWarcinfoIdUri);
			Assert.assertEquals(warcResourceId, header.warcRecordIdUri);
			Assert.assertNull(header.warcRefersToUri);

			pbin = record.getPayload().getInputStream();
			out.reset();
			while ((read = pbin.read(tmpBuf)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			out.close();
			pbin.close();
			Assert.assertArrayEquals(dataBytes, out.toByteArray());

			record = reader.getNextRecord();
			Assert.assertNotNull(record);
			Assert.assertTrue(record.isCompliant());
			header = record.header;
			Assert.assertEquals(new Integer(WarcConstants.RT_IDX_METADATA), header.warcTypeIdx);
			Assert.assertEquals(warcinfoId, header.warcWarcinfoIdUri);
			Assert.assertEquals(warcMetadataId, header.warcRecordIdUri);
			Assert.assertEquals(warcResourceId, header.warcRefersToUri);

			pbin = record.getPayload().getInputStream();
			out.reset();
			while ((read = pbin.read(tmpBuf)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			out.close();
			pbin.close();
			Assert.assertArrayEquals(metadataBytes, out.toByteArray());

			record = reader.getNextRecord();
			Assert.assertNull(record);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (YggdrasilException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

}
