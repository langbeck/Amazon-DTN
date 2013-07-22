/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpa.adtn.storage.sqlite;

import java.util.Iterator;

import br.ufpa.adtn.storage.EntityStorage;
import br.ufpa.adtn.storage.Identificable;
import br.ufpa.adtn.storage.StorageException;

public abstract class SQLiteEntityStorage<T extends Identificable> implements EntityStorage<T> {
	private SQLiteAdapter adapter;
	
	@Override
	public Iterator<T> iterator() {
		return new CursorIterator(adapter.list());
	}
	
	@Override
	public T load(long id) throws StorageException {
		try {
			final SQLiteCursor cursor = adapter.load(id);
			if (!cursor.next())
				return null;
	
			return deserialize(cursor);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
	
	@Override
	public void save(T o) throws StorageException {
		try {
			o.setId(adapter.save(serialize(o)));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
	
	@Override
	public void update(T o) throws StorageException {
		try {
			adapter.update(o.getId(), serialize(o));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
	
	protected abstract T deserialize(SQLiteCursor cursor);
	protected abstract SQLiteEntry serialize(T o);
	
	
	private class CursorIterator implements Iterator<T> {
		private final SQLiteCursor cursor;
		
		public CursorIterator(SQLiteCursor cursor) {
			this.cursor = cursor;
		}

		@Override
		public boolean hasNext() {
			return cursor.next();
		}

		@Override
		public T next() {
			return deserialize(cursor);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public static final class SQLiteEntry {
		private final String[] columns;
		private final Object[] data;
		
		public SQLiteEntry(String[] columns, Object[] data) {
			if (columns.length != data.length || data.length == 0)
				throw new IllegalArgumentException();
			
			this.columns = columns;
			this.data = data;
		}
		
		public void asInsert(StringBuilder buffer) {
			buffer.append(" (");
			for (int i = 0, len = columns.length; i < len; i++)
				buffer.append("\"").append(columns[i]).append("\"");
			
			buffer.append(") VALUES (");

			for (int i = 0, len = data.length; i < len; i++) {
				final Object o = data[i];
				if (o instanceof String) {
					SQLiteUtil.escapeString((String) o, buffer.append("\""));
					buffer.append("\"");
				} else if (o instanceof Number) {
					buffer.append(o.toString());
				} else {
					throw new IllegalArgumentException("Invalid data type.");
				}
				

				if (i < len - 1)
					buffer.append(", ");
			}
			
			buffer.append(")");
		}
		
		public void asUpdate(StringBuilder buffer) {
			buffer.append(" SET ");
			for (int i = 0, len = columns.length; i < len; i++) {
				buffer.append("\"").append(columns[i]).append("\" = ");
				
				final Object o = data[i];
				if (o instanceof String) {
					SQLiteUtil.escapeString((String) o, buffer.append("\""));
					buffer.append("\"");
				} else if (o instanceof Number) {
					buffer.append(o.toString());
				} else {
					throw new IllegalArgumentException("Invalid data type.");
				}
				
				if (i < len - 1)
					buffer.append(" AND ");
			}
		}
	}
}
