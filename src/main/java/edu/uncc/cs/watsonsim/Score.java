package edu.uncc.cs.watsonsim;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.uncc.cs.watsonsim.scorers.Merge;

/**
 * Represent how to create and merge a score.
 * This is mostly autogenerated.
 * @author Sean
 */
final class Meta implements Comparable<Meta> {
	public final String name;
	public final double default_value;
	public final Merge merge_type;
	public Meta(String name, double default_value, Merge merge_type) {
		this.name = name;
		this.default_value = default_value;
		this.merge_type = merge_type;
	}
	@Override
	public int compareTo(Meta o) {
		if (o == null) return 0;
		return o.name.compareTo(name);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Meta other = (Meta) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
}
/**
 * Namespace for managing score vectors.
 * 
 * The score vectors are designed to be memory efficient.
 * So they have no objects or pointers; only primitives.
 * You can manage them using static methods in this class.
 * @author Sean
 */
public class Score extends HashMap<String, Double> implements Map<String, Double> {
	private static final long serialVersionUID = 3368114859528405852L;
	private static final SortedMap<String, Meta> template = new ConcurrentSkipListMap<>();

	static {
		// This means the length of the incoming double[] is the same as
		// the index into versions[].
		register("COUNT", 1, Merge.Sum);
	}
	
	public Score() {
		super();
	}
	

	/**
	 * Returns a convenient copy of scores as a map.
	 * @param scores
	 * @return
	 */
	public static Map<String, Double> asMap(Score scores) {
		return scores;
	}
	
	/**
	 * Get a "blank" vector (all defaults)
	 */
	public static Score empty() {
		return new Score();
	}

	/**
	 * Get a specific score
	 * 
	 * @param scores	The score vector
	 * @param name		The name of the score
	 */
	public double get(String name) {
		if (template.containsKey(name))
			return getOrDefault(name, template.get(name).default_value);
		else
			return 0.0;
	}
	/**
	 * Get a bunch of scores in a new order.
	 * There is no going back!
	 * You can't get() or set() or update() the output of this function!
	 * @param incoming
	 * @param names
	 * @return
	 */
	public double[] getEach(Collection<String> names) {
		double[] outgoing = new double[names.size()];
		int i=0;
		for (String name : names) {
			outgoing[i] = get(name);
			i++;
		}
		return outgoing;
	}
	
	public static Set<String> latestSchema() {
		return template.keySet();
	}
	
	/**
	 * Merge two scores
	 */
	public static Score merge(Score left, Score right) {
		double left_count = left.get("COUNT"),
		       right_count = right.get("COUNT");
		if (left_count + right_count > 0) {
			Score center = new Score();
			for ( Meta m : template.values() ) {
				switch (m.merge_type) {
				case Mean:
					double val = left_count * left.get(m.name)
						+ right_count * right.get(m.name);
					val /= left_count + right_count;
					center.put(m.name, val);
					break;
				case Or: 
					center.put(m.name, left.get(m.name) + right.get(m.name) > 0 ? 1.0 : 0.0);
					break;
				case Min:
					center.put(m.name, Math.min(left.get(m.name), right.get(m.name))); break;
				case Max:
					center.put(m.name, Math.max(left.get(m.name), right.get(m.name))); break;
				case Sum:
					center.put(m.name, left.get(m.name) + right.get(m.name)); break; 
				}
			}
			return center;
		} else {
			Score nscore = new Score();
			nscore.putAll(left);
			return nscore;
		}
	}
	
	/**
	 * Normalize a set of scores against one another.
	 * This is intended to be run once per question.
	 * Afterward, the mean will be 0 and the stdev 1.
	 */
	public static List<Answer> normalizeGroup(List<Answer> mat) {
		
		Set<String> keys = template.keySet();
		final int len = keys.size();
		String[] keysarr = new String[len];
		int k_idx = 0;
		for (String k: keys) {
			keysarr[k_idx] = k; k_idx++;
		}
		int preserve_attr = Arrays.binarySearch(keysarr, "CORRECT");
		
		double[] sum = new double[keys.size()];
		// Generate sum
		for (Answer row : mat) {
			for (int i=0; i<len; i++) {
				sum[i] += row.scores.get(keysarr[i]);
			}
		}
		// Make sum an average
		for (int i=0; i<len; i++) {
			sum[i] /= mat.size();
		}
		// Generate variance
		double[] variance = new double[len];
		for (Answer row : mat) {
			for (int i=0; i<len; i++) {
				double diff = sum[i] - row.scores.get(keysarr[i]);
				variance[i] += diff * diff;
			}
		}
		// Generate stdev
		double[] stdev = variance.clone();
		for (int i=0; i<len; i++) {
			stdev[i] = Math.sqrt(stdev[i]);
		}
		// Scale the copy
		for (Answer row: mat) {
			for (int col=0; col<len; col++) {
				if (col != preserve_attr
						&& stdev[col] != 0) {
					row.scores.put(keysarr[col], (row.scores.get(keysarr[col]) - sum[col]) / stdev[col]);
				}
			}
		}
		return mat;
	}
	
	/** Register the answer score for automatically generated model data
	 * 
	 * This function is idempotent.
	 * @param name		The name of the score as it will be presented to Weka
	 * @param default_value		What the value of the score should be if it is missing
	 * @param merge_mode		How to merge two scores of the same name
	 */
	public static void register(String name,
			double default_value,
			Merge merge_mode) {
		template.putIfAbsent(name,
				new Meta(name, default_value, merge_mode));
	}
	
	public Score clone() {
		Score s = new Score();
		s.putAll(this);
		return s;
	}
}
