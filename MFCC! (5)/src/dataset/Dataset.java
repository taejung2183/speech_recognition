package dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Dataset {
	private List<Record> records;
	private int lableIndex;

	private double maxLable = -1;

	public Dataset(int classIndex) {

		this.lableIndex = classIndex;
		records = new ArrayList<Record>();
	}

	public Dataset(List<double[]> datas) {
		this();
		for (double[] data : datas) {
			append(new Record(data));
		}
	}

	private Dataset() {
		this.lableIndex = -1;
		records = new ArrayList<Record>();
	}

	public int size() {
		return records.size();
	}

	public int getLableIndex() {
		return lableIndex;
	}

	public void append(Record record) {
		records.add(record);
	}

	public void clear() {
		records.clear();
	}

	public void append(double[] attrs, Double lable) {
		records.add(new Record(attrs, lable));
	}

	public Iterator<Record> iter() {
		return records.iterator();
	}

	public double[] getAttrs(int index) {
		return records.get(index).getAttrs();
	}

	public Double getLable(int index) {
		return records.get(index).getLable();
	}

	public static Dataset load(String filePath, String tag, int lableIndex) {
		Dataset dataset = new Dataset();
		dataset.lableIndex = lableIndex;
		File file = new File(filePath);
		try {

			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				String[] datas = line.split(tag);
				System.out.println(datas.length);
				if (datas.length == 0)
					continue;
				double[] data = new double[datas.length];
				for (int i = 0; i < datas.length; i++)
					data[i] = Double.parseDouble(datas[i]);
				Record record = dataset.new Record(data);
				dataset.append(record);
			}
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		System.out.println("��������:" + dataset.size());
		return dataset;
	}

	public class Record {
		private double[] attrs;
		private Double lable;

		private Record(double[] attrs, Double lable) {
			this.attrs = attrs;
			this.lable = lable;
		}

		public Record(double[] data) {
			if (lableIndex == -1)
				attrs = data;
			else {
				System.out.println(data.length);
				lable = data[lableIndex];
				if (lable > maxLable)
					maxLable = lable;
				if (lableIndex == 0)
					attrs = Arrays.copyOfRange(data, 1, data.length);
				else
					attrs = Arrays.copyOfRange(data, 0, data.length - 1);
			}
		}

		public double[] getAttrs() {
			return attrs;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("attrs:");
			sb.append(Arrays.toString(attrs));
			sb.append("lable:");
			sb.append(lable);
			return sb.toString();
		}

		public Double getLable() {
			if (lableIndex == -1)
				return null;
			return lable;
		}

		public int[] getEncodeTarget(int n) {
			String binary = Integer.toBinaryString(lable.intValue());
			byte[] bytes = binary.getBytes();
			int[] encode = new int[n];
			int j = n;
			for (int i = bytes.length - 1; i >= 0; i--)
				encode[--j] = bytes[i] - '0';

			return encode;
		}

		public double[] getDoubleEncodeTarget(int n) {
			String binary = Integer.toBinaryString(lable.intValue());
			byte[] bytes = binary.getBytes();
			double[] encode = new double[n];
			int j = n;
			for (int i = bytes.length - 1; i >= 0; i--)
				encode[--j] = bytes[i] - '0';

			return encode;
		}

	}

	public static void main(String[] args) {
		Dataset d = new Dataset();
		d.lableIndex = 10;
		Record r = d.new Record(new double[] { 3, 2, 2, 5, 4, 5, 3, 11, 3, 12,
				1 });
		int[] encode = r.getEncodeTarget(4);

		System.out.println(r.lable);
		System.out.println(Arrays.toString(encode));
	}

	public Record getRecord(int index) {
		return records.get(index);
	}

}
