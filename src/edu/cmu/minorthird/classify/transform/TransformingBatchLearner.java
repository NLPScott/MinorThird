/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.ProgressCounter;

import java.util.*;

/**
 * Learns to first transforming data with an InstanceTransform, then
 * classify it.
 *
 * @author William Cohen
 */

public class TransformingBatchLearner extends BatchClassifierLearner
{
	private InstanceTransformLearner transformLearner;
	private BatchClassifierLearner classifierLearner;

	public TransformingBatchLearner()
	{
		this(new FrequencyBasedTransformLearner(3), new LogisticRegressor());
	}

	public void setTransformLearner(InstanceTransformLearner learner) { transformLearner=learner; }
	public InstanceTransformLearner getTransformLearner() { return transformLearner; }

	public void setClassifierLearner(BatchClassifierLearner learner) { classifierLearner=learner; }
	public BatchClassifierLearner getClassifierLearner() { return classifierLearner; }

	public void setSchema(ExampleSchema schema)
	{
		classifierLearner.setSchema(schema);
	}

	public TransformingBatchLearner(
		InstanceTransformLearner transformLearner,
		BatchClassifierLearner classifierLearner)
	{
		this.transformLearner = transformLearner;
		this.classifierLearner = classifierLearner;
	}

	public Classifier batchTrain(Dataset dataset)
	{
		final InstanceTransform transformer = transformLearner.batchTrain(dataset);
		final Classifier classifier = classifierLearner.batchTrain(transformer.transform(dataset));

		return new Classifier () {
				public ClassLabel classification(Instance instance) {
					return classifier.classification( transformer.transform(instance) );
				}
				public String explain(Instance instance) {
					Instance transformedInstance = transformer.transform(instance);
					return
						"Transformed instance: "+transformedInstance+"\n"+
						classifier.explain(transformedInstance)+"\n";
				}
			};
	}
}
