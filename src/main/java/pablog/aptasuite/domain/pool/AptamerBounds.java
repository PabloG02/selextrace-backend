package pablog.aptasuite.domain.pool;

import java.io.Serializable;

/**
 * @author Jan Hoinka
 * Simple class representing the start index (inclusive) and
 * end index (exclusive) of the randomized region of an aptamer
 */
public class AptamerBounds implements Serializable {
	public int startIndex;
	public int endIndex;

	/**
	 * No-argument constructor needed for framework deserialization (e.g., MongoDB).
	 */
	protected AptamerBounds() {
		this.startIndex = 0;
		this.endIndex = 0;
	}
	
	/**
	 * Creates a new AptamerBounds instance 
	 * @param start the start position of the randomized region (inclusive)
	 * @param end the end position of the randomized region (exclusive)
	 */
	public AptamerBounds(int start, int end){
		this.startIndex = start;
		this.endIndex = end;
	}
	
	/**
	 * Creates a new AptamerBounds instance 
	 * @param indices array of size 2 with position 0 representing the start 
	 * position of the randomized region (inclusive), and position 1 the 
	 * end position of the randomized region (exclusive)
	 */
	public AptamerBounds(int[] indices){
		this.startIndex = indices[0];
		this.endIndex = indices[1];
	}
}
