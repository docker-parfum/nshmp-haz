package org.opensha2.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkState;
import static org.opensha2.data.Data.checkDelta;
import static org.opensha2.util.TextUtils.NEWLINE;

import java.util.Arrays;
import java.util.List;

import org.opensha2.util.Parsing;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;

/**
 * Static utilities for working with and concrete implementations of 2D and 3D
 * data containers.
 *
 * @author Peter Powers
 */
public final class DataTables {

	/**
	 * Create a set of keys for use in a {@link DataTable} or {@link DataVolume}
	 * . Both these classes call this method directly when initializing their
	 * backing arrays. It is exposed for convenience as there are circumstances
	 * where a reference to the row or column keys is helpful to have when
	 * working with data table and volume builders. Internally, this method
	 * calls
	 * {@link Data#buildCleanSequence(double, double, double, boolean, int)}
	 * wiht a precision value of 4 decimal places. This may change in the future
	 * 
	 * <p><b>Example:</b> {@code keys(5.0, 8.0, 1.0)} returns [5.5, 6.5, 7.5]</p>
	 * 
	 * @param min lower edge of lowermost bin
	 * @param max upper edge of uppermost bin
	 * @param Δ bin width
	 */
	public static double[] keys(double min, double max, double Δ) {
		return keyArray(min, max, checkDelta(min, max, Δ));
	}

	/*
	 * Create clean sequence of keys. Precision is curently set to 4 decimal
	 * places.
	 */
	private static double[] keyArray(double min, double max, double Δ) {
		double Δby2 = Δ / 2.0;
		return Data.buildCleanSequence(
			min + Δby2,
			max - Δby2,
			Δ, true, 4);
	}

	/*
	 * Compute an index from a minimum value, a value and an interval. Casting
	 * to int floors value.
	 */
	static int indexOf(double min, double delta, double value, int size) {
		return checkElementIndex((int) ((value - min) / delta), size);
	}

	private static void checkDataState(double[] data, String label) {
		checkState(data != null, "%s data have not yet been fully specified", label);
	}

	/*
	 * Ensure rows and columns have been specified
	 */
	static void checkDataState(double[] rows, double[] columns) {
		checkDataState(rows, "Row");
		checkDataState(columns, "Column");
	}

	/*
	 * Ensure rows and columns have been specified
	 */
	static void checkDataState(double[] rows, double[] columns, double[] levels) {
		checkDataState(rows, columns);
		checkDataState(levels, "Level");
	}

	/*
	 * Confirm that data array conforms to the row and column sizes already
	 * configured.
	 */
	static void checkDataSize(int rowSize, int columnSize, double[][] data) {
		checkArgument(
			data.length == rowSize,
			"Expected %s rows of data but only %s were supplied",
			rowSize, data.length);
		for (int i = 0; i < data.length; i++) {
			double[] column = data[i];
			checkArgument(
				column.length == columnSize,
				"Expected %s columns but only %s were supplied on row %s",
				columnSize, column.length, i);
		}
	}

	/*
	 * Confirm that data array conforms to the row and column sizes already
	 * configured.
	 */
	static void checkDataSize(int rowSize, int columnSize, int levelSize, double[][][] data) {
		checkArgument(
			data.length == rowSize,
			"Expected %s rows of data but only %s were supplied",
			rowSize, data.length);
		for (int i = 0; i < data.length; i++) {
			double[][] column = data[i];
			checkArgument(
				column.length == columnSize,
				"Expected %s columns but only %s were supplied on row %s",
				columnSize, column.length, i);
			for (int j = 0; j < column.length; j++) {
				double[] level = column[j];
				checkArgument(
					level.length == levelSize,
					"Expected %s levels but only %s were supplied on row %s, column %s",
					levelSize, level.length, i, j);
			}
		}
	}

	static abstract class AbstractTable implements DataTable {

		final double rowMin;
		final double rowMax;
		final double rowΔ;
		final double[] rows;

		final double columnMin;
		final double columnMax;
		final double columnΔ;
		final double[] columns;

		private AbstractTable(
				double rowMin, double rowMax, double rowΔ, double[] rows,
				double columnMin, double columnMax, double columnΔ, double[] columns) {

			this.rowMin = rowMin;
			this.rowMax = rowMax;
			this.rowΔ = rowΔ;
			this.rows = rows;

			this.columnMin = columnMin;
			this.columnMax = columnMax;
			this.columnΔ = columnΔ;
			this.columns = columns;
		}

		@Override public List<Double> rows() {
			return ImmutableList.copyOf(Doubles.asList(rows));
		}

		@Override public List<Double> columns() {
			return ImmutableList.copyOf(Doubles.asList(columns));
		}

	}

	static final class DefaultTable extends AbstractTable {

		final double[][] data;

		DefaultTable(
				double rowMin, double rowMax, double rowΔ, double[] rows,
				double columnMin, double columnMax, double columnΔ, double[] columns,
				double[][] data) {

			super(
				rowMin, rowMax, rowΔ, rows,
				columnMin, columnMax, columnΔ, columns);
			this.data = data;
		}

		@Override public double get(final double row, final double column) {
			int iRow = indexOf(rowMin, rowΔ, row, rows.length);
			int iColumn = indexOf(columnMin, columnΔ, column, columns.length);
			return data[iRow][iColumn];
		}

		private static final String ROW_COL_FORMAT = "% 8.2f";
		private static final String DATA_FORMAT = "%7.2e";
		private static final String ZEROS_IN = "0.00e+00";
		private static final String ZEROS_OUT = "     0.0";
		private static final String DELIMITER = ", ";

		@Override public String toString() {
			StringBuilder sb = new StringBuilder();
			List<Double> rows = rows();
			sb.append("           ");
			sb.append(Parsing.toString(columns(), ROW_COL_FORMAT, DELIMITER, true));
			sb.append(NEWLINE);
			for (int i = 0; i < data.length; i++) {
				sb.append("[");
				sb.append(String.format(ROW_COL_FORMAT, rows.get(i)));
				sb.append("] ");
				// format as scientific but replace zeros
				List<Double> dataRow = Doubles.asList(data[i]);
				String dataLine = Parsing.toString(dataRow, DATA_FORMAT, DELIMITER, true);
				dataLine = dataLine.replace(ZEROS_IN, ZEROS_OUT);
				sb.append(dataLine);
				sb.append(NEWLINE);
			}
			return sb.toString();
		}

		@Override public XySequence row(double row) {
			int iRow = indexOf(rowMin, rowΔ, row, rows.length);
			return new ImmutableXySequence(columns, data[iRow]);
		}
	}

	static final class SingularTable extends AbstractTable {

		final double value;
		private final double[] row;

		SingularTable(
				double rowMin, double rowMax, double rowΔ, double[] rows,
				double columnMin, double columnMax, double columnΔ, double[] columns,
				double value) {

			super(
				rowMin, rowMax, rowΔ, rows,
				columnMin, columnMax, columnΔ, columns);
			this.value = value;
			this.row = new double[columns.length];
			Arrays.fill(this.row, value);
		}

		@Override public double get(final double rowKey, final double columnKey) {
			return value;
		}

		@Override public XySequence row(double row) {
			return new ImmutableXySequence(columns, this.row);
		}
	}

	static abstract class AbstractVolume implements DataVolume {

		final double rowMin;
		final double rowMax;
		final double rowΔ;
		final double[] rows;

		final double columnMin;
		final double columnMax;
		final double columnΔ;
		final double[] columns;

		final double levelMin;
		final double levelMax;
		final double levelΔ;
		final double[] levels;

		private AbstractVolume(
				double rowMin, double rowMax, double rowΔ, double[] rows,
				double columnMin, double columnMax, double columnΔ, double[] columns,
				double levelMin, double levelMax, double levelΔ, double[] levels) {

			this.rowMin = rowMin;
			this.rowMax = rowMax;
			this.rowΔ = rowΔ;
			this.rows = rows;

			this.columnMin = columnMin;
			this.columnMax = columnMax;
			this.columnΔ = columnΔ;
			this.columns = columns;

			this.levelMin = levelMin;
			this.levelMax = levelMax;
			this.levelΔ = levelΔ;
			this.levels = levels;
		}

		@Override public List<Double> rows() {
			return ImmutableList.copyOf(Doubles.asList(rows));
		}

		@Override public List<Double> columns() {
			return ImmutableList.copyOf(Doubles.asList(columns));
		}

		@Override public List<Double> levels() {
			return ImmutableList.copyOf(Doubles.asList(levels));
		}
	}

	static final class DefaultVolume extends AbstractVolume {

		final double[][][] data;

		DefaultVolume(
				double rowMin, double rowMax, double rowΔ, double[] rows,
				double columnMin, double columnMax, double columnΔ, double[] columns,
				double levelMin, double levelMax, double levelΔ, double[] levels,
				double[][][] data) {

			super(
				rowMin, rowMax, rowΔ, rows,
				columnMin, columnMax, columnΔ, columns,
				levelMin, levelMax, levelΔ, levels);
			this.data = data;
		}

		@Override public double get(final double row, final double column, final double level) {
			int iRow = indexOf(rowMin, rowΔ, row, rows.length);
			int iColumn = indexOf(columnMin, columnΔ, column, columns.length);
			int iLevel = indexOf(levelMin, levelΔ, level, levels.length);
			return data[iRow][iColumn][iLevel];
		}

		@Override public XySequence column(double row, double column) {
			int iRow = indexOf(rowMin, rowΔ, row, rows.length);
			int iColumn = indexOf(columnMin, columnΔ, column, columns.length);
			return new ImmutableXySequence(levels, data[iRow][iColumn]);
		}
	}

}
