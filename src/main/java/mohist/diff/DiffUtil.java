package mohist.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Stack;

public class DiffUtil {

	@FunctionalInterface
	public interface KeyProvider<T> {
		int key(T t);
	}

	public static final int NotFound = Integer.MAX_VALUE;

	public static class Result<Integer> {
		public ArrayList<Integer> reloadList = new ArrayList<>();
		public ArrayList<Integer> addedList = new ArrayList<>();
		public ArrayList<Integer> deletedList = new ArrayList<>();
		public ArrayList<Integer> fromList = new ArrayList<>();
		public ArrayList<Integer> toList = new ArrayList<>();

		public ArrayList<Integer> getAddedList() {
			return this.addedList;
		}

		public void toAdd(Integer item) {
			this.addedList.add(item);
		}

		public void toDelete(Integer item) {
			this.deletedList.add(item);
		}

		public void move(Integer from, Integer to) {
			this.fromList.add(from);
			this.toList.add(to);
		}

	}

	public static class Record {
		public Entry entry;
		public int index = NotFound;

		Record(Entry entry) {
			this.entry = entry;
		}
	}

	public static class Entry {
		private int oldCounter = 0;
		private int newCounter = 0;
		private Stack<Integer> stack = new Stack<>();

		public Entry increaseNew() {
			this.newCounter++;
			this.stack.push(NotFound);
			return this;
		}

		public int pop() {
			return this.stack.pop();
		}

		public Entry increaseOld(int index) {
			this.oldCounter++;
			this.stack.push(index);
			return this;
		}

		public boolean isNew() {
			return this.oldCounter == 0;
		}

		public boolean isOut() {
			return this.newCounter == 0;
		}
	}

	public static <T> Result<T> diff(List<T> oldList, List<T> newList, KeyProvider<? super T> keyProvider) {

		HashMap<Integer, Entry> hashMap = new HashMap<>();

		ArrayList<Record> newRecords = new ArrayList<>();
		// step 1
		for (T i : newList) {
			int key = keyProvider.key(i);
			Entry entry = hashMap.get(key);
			if (entry == null) {
				entry = new Entry().increaseNew();
				hashMap.put(key, entry);
			} else {
				entry.increaseNew();
			}
			newRecords.add(new Record(entry));
		}

		ArrayList<Record> oldRecords = new ArrayList<>();
		// step 2
		for (int i = oldList.size() - 1; i >= 0; i--) {
			int key = keyProvider.key(oldList.get(i));
			Entry entry = hashMap.get(key);
			if (entry == null) {
				entry = new Entry().increaseOld(i);
				hashMap.put(key, entry);
			} else {
				entry.increaseOld(i);
			}
			oldRecords.add(0, new Record(entry));
		}

		Result result = new Result();

		// step 3
		for (int i = 0; i < newRecords.size(); i++) {
			Record record = newRecords.get(i);
			int originIndex = record.entry.pop();
			if (originIndex != NotFound) {
				oldRecords.get(originIndex).index = i;
				record.index = originIndex;
			}
		}

		int[] deleteOffsets = new int[oldRecords.size()];
		int deleteOffset = 0;
		// step 4
		for (int i = 0; i < oldRecords.size(); i++) {
			Record record = oldRecords.get(i);
			deleteOffsets[i] = deleteOffset;
			if (record.index == NotFound) {
				result.toDelete(i);
				deleteOffset++;
			}

		}

		int[] newOffsets = new int[newRecords.size()];
		int newOffset = 0;
		// step 5
		for (int i = 0; i < newRecords.size(); i++) {
			newOffsets[i] = newOffset;
			Record record = newRecords.get(i);
			if (record.index == NotFound) {
				result.toAdd(i);
				newOffset++;
			} else if (record.index - deleteOffsets[record.index] + newOffsets[i] != i) {
				result.move(record.index, i);
			}
		}

		return result;

	}

}
