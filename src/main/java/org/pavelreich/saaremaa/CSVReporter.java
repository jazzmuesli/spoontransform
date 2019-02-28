package org.pavelreich.saaremaa;

import java.io.IOException;

import org.apache.commons.csv.CSVPrinter;

/**
 * Wrapper for final CSVPrinter (that's why people need Powermock!).
 * 
 * @author preich
 *
 */
public class CSVReporter {

	private CSVPrinter csvPrinter;

	public CSVReporter(CSVPrinter csvPrinter) {
		this.csvPrinter = csvPrinter;
	}

	public synchronized void write(Object... values) {
		try {
			csvPrinter.printRecord(values);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public synchronized void flush() {
		try {
			csvPrinter.flush();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public synchronized void close() {
		try {
			csvPrinter.close();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}