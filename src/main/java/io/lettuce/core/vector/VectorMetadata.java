/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.vector;

/**
 * Represents metadata and internal details about a Redis vector set.
 * <p>
 * This class encapsulates information returned by the Redis VINFO command, including size, dimensions, quantization type, and
 * graph structure parameters of a vector set.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see <a href="https://redis.io/docs/latest/commands/vinfo/">Redis Documentation: VINFO</a>
 */
public class VectorMetadata {

    private Integer dimensionality;

    private QuantizationType type;

    private Integer size;

    private Integer maxNodeUid;

    private Integer vSetUid;

    private Integer maxNodes;

    private Integer projectionInputDim;

    private Integer attributesCount;

    /**
     * Creates a new empty {@link VectorMetadata} instance.
     * <p>
     * This constructor creates an empty metadata object. The fields will be populated when the object is used to parse the
     * response from a Redis VINFO command.
     */
    public VectorMetadata() {
        // Default constructor
    }

    /**
     * Gets the dimensionality of the vectors in the vector set.
     *
     * @return the number of dimensions for each vector in the vector set
     */
    public Integer getDimensionality() {
        return dimensionality;
    }

    /**
     * Gets the quantization type used for storing vectors in the vector set.
     *
     * @return the quantization type (Q8, NOQUANT, or BIN)
     */
    public QuantizationType getType() {
        return type;
    }

    /**
     * Gets the number of elements (vectors) in the vector set.
     *
     * @return the total count of vectors in the vector set
     */
    public Integer getSize() {
        return size;
    }

    public Integer getMaxNodeUid() {
        return maxNodeUid;
    }

    /**
     * Gets the unique identifier of the vector set.
     *
     * @return the unique identifier used by Redis to track the vector set
     */
    public Integer getvSetUid() {
        return vSetUid;
    }

    /**
     * Gets the maximum number of connections per node in the HNSW graph.
     * <p>
     * This parameter (also known as M) specifies the maximum number of connections that each node of the graph will have with
     * other nodes.
     *
     * @return the maximum number of connections per node
     */
    public Integer getMaxNodes() {
        return maxNodes;
    }

    public Integer getProjectionInputDim() {
        return projectionInputDim;
    }

    public Integer getAttributesCount() {
        return attributesCount;
    }

    /**
     * Sets the dimensionality of the vectors in the vector set.
     *
     * @param dimensionality the number of dimensions for each vector in the vector set
     */
    public void setDimensionality(Integer dimensionality) {
        this.dimensionality = dimensionality;
    }

    /**
     * Sets the quantization type used for storing vectors in the vector set.
     *
     * @param type the quantization type (Q8, NOQUANT, or BIN)
     */
    public void setType(QuantizationType type) {
        this.type = type;
    }

    /**
     * Sets the number of elements (vectors) in the vector set.
     *
     * @param size the total count of vectors in the vector set
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * Sets the exploration factor (EF) used for the vector set.
     * <p>
     * The EF parameter controls the search effort in the HNSW algorithm.
     *
     * @param explorationFactor the exploration factor value
     */
    public void maxNodeUid(Integer explorationFactor) {
        this.maxNodeUid = explorationFactor;
    }

    /**
     * Sets the unique identifier of the vector set.
     *
     * @param vSetUid the unique identifier used by Redis to track the vector set
     */
    public void setvSetUid(Integer vSetUid) {
        this.vSetUid = vSetUid;
    }

    /**
     * Sets the maximum number of connections per node in the HNSW graph.
     * <p>
     * This parameter (also known as M) specifies the maximum number of connections that each node of the graph will have with
     * other nodes.
     *
     * @param maxNodes the maximum number of connections per node
     */
    public void setMaxNodes(Integer maxNodes) {
        this.maxNodes = maxNodes;
    }

    public void setProjectionInputDim(Integer value) {
        this.projectionInputDim = value;
    }

    public void setAttributesCount(Integer value) {
        this.attributesCount = value;
    }

}
