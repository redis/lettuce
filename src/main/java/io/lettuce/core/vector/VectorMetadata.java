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
 * <p>
 * Vector sets are a Redis data type designed for storing and searching high-dimensional vectors. They support approximate
 * nearest neighbor search using the HNSW (Hierarchical Navigable Small World) algorithm.
 * <p>
 * The metadata provides insights into the configuration and state of a vector set, which can be useful for monitoring,
 * debugging, and understanding the performance characteristics of vector operations.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see <a href="https://redis.io/docs/latest/commands/vinfo/">Redis Documentation: VINFO</a>
 * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/">Redis Documentation: Vector Sets</a>
 */
public class VectorMetadata {

    /**
     * The dimensionality of the vectors in the vector set.
     * <p>
     * This represents the number of dimensions (features) in each vector. All vectors in a vector set must have the same
     * dimensionality. Higher dimensionality vectors can represent more complex data but require more memory and processing
     * power.
     * <p>
     * Corresponds to the {@code vector-dim} field in the VINFO command response.
     */
    private Integer dimensionality;

    /**
     * The quantization type used for storing vectors in the vector set.
     * <p>
     * Quantization affects how vectors are stored and impacts memory usage, performance, and recall quality.
     * <p>
     * Common types include:
     * <ul>
     * <li>{@code Q8} - Uses signed 8-bit quantization, balancing memory usage and recall quality</li>
     * <li>{@code NOQUANT} - Stores vectors without quantization, using more memory but preserving full precision</li>
     * <li>{@code BIN} - Uses binary quantization, which is faster and uses less memory, but impacts recall quality</li>
     * </ul>
     * <p>
     * Corresponds to the {@code quant-type} field in the VINFO command response.
     */
    private QuantizationType type;

    /**
     * The number of elements (vectors) in the vector set.
     * <p>
     * This represents the total count of vectors currently stored in the vector set.
     * <p>
     * Corresponds to the {@code size} field in the VINFO command response.
     */
    private Integer size;

    /**
     * The maximum node UID in the HNSW graph.
     * <p>
     * This is an internal identifier used by Redis to track nodes in the HNSW graph.
     * <p>
     * Corresponds to the {@code hnsw-max-node-uid} field in the VINFO command response.
     */
    private Integer maxNodeUid;

    /**
     * The unique identifier of the vector set.
     * <p>
     * This is an internal identifier used by Redis to track the vector set.
     * <p>
     * Corresponds to the {@code vset-uid} field in the VINFO command response.
     */
    private Integer vSetUid;

    /**
     * The maximum number of connections per node in the HNSW graph.
     * <p>
     * This parameter (also known as M) specifies the maximum number of connections that each node of the graph will have with
     * other nodes. More connections means more memory, but provides for more efficient graph exploration.
     * <p>
     * Higher values improve recall at the cost of memory usage and indexing speed.
     * <p>
     * Corresponds to the {@code M} field in the VINFO command response.
     */
    private Integer maxNodes;

    /**
     * The original dimensionality of vectors before dimensionality reduction.
     * <p>
     * When using the REDUCE option with VADD, this field indicates the original dimensionality of the vectors before they were
     * projected to a lower-dimensional space.
     * <p>
     * Corresponds to the {@code projection-input-dim} field in the VINFO command response.
     */
    private Integer projectionInputDim;

    /**
     * The number of elements in the vector set that have attributes.
     * <p>
     * Attributes are JSON metadata associated with vector elements that can be used for filtering in similarity searches.
     * <p>
     * Corresponds to the {@code attributes-count} field in the VINFO command response.
     */
    private Integer attributesCount;

    /**
     * The maximum level in the HNSW graph.
     * <p>
     * The HNSW algorithm organizes vectors in a hierarchical graph with multiple levels. This parameter indicates the highest
     * level in the graph structure.
     * <p>
     * Higher levels enable faster navigation through the graph during similarity searches.
     * <p>
     * Corresponds to the {@code max-level} field in the VINFO command response.
     */
    private Integer maxLevel;

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
     * <p>
     * This represents the number of dimensions (features) in each vector. All vectors in a vector set must have the same
     * dimensionality. Higher dimensionality vectors can represent more complex data but require more memory and processing
     * power.
     *
     * @return the number of dimensions for each vector in the vector set, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/memory/">Memory Optimization</a>
     */
    public Integer getDimensionality() {
        return dimensionality;
    }

    /**
     * Gets the quantization type used for storing vectors in the vector set.
     * <p>
     * Quantization affects how vectors are stored and impacts memory usage, performance, and recall quality.
     * <p>
     * Common types include:
     * <ul>
     * <li>{@code Q8} - Uses signed 8-bit quantization, balancing memory usage and recall quality</li>
     * <li>{@code NOQUANT} - Stores vectors without quantization, using more memory but preserving full precision</li>
     * <li>{@code BIN} - Uses binary quantization, which is faster and uses less memory, but impacts recall quality</li>
     * </ul>
     *
     * @return the quantization type used for the vector set, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/performance/#quantization-effects">Quantization
     *      Effects</a>
     */
    public QuantizationType getType() {
        return type;
    }

    /**
     * Gets the number of elements (vectors) in the vector set.
     * <p>
     * This represents the total count of vectors currently stored in the vector set. This is equivalent to the result of the
     * {@code VCARD} command.
     *
     * @return the total count of vectors in the vector set, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/commands/vcard/">VCARD Command</a>
     */
    public Integer getSize() {
        return size;
    }

    /**
     * Gets the maximum node UID in the HNSW graph.
     * <p>
     * This is an internal identifier used by Redis to track nodes in the HNSW graph. It can be useful for debugging and
     * monitoring purposes.
     *
     * @return the maximum node UID in the HNSW graph, or {@code null} if not available
     */
    public Integer getMaxNodeUid() {
        return maxNodeUid;
    }

    /**
     * Gets the unique identifier of the vector set.
     * <p>
     * This is an internal identifier used by Redis to track the vector set. It can be useful for debugging and monitoring
     * purposes.
     *
     * @return the unique identifier used by Redis to track the vector set, or {@code null} if not available
     */
    public Integer getvSetUid() {
        return vSetUid;
    }

    /**
     * Gets the maximum number of connections per node in the HNSW graph.
     * <p>
     * This parameter (also known as M) specifies the maximum number of connections that each node of the graph will have with
     * other nodes. More connections means more memory, but provides for more efficient graph exploration.
     * <p>
     * Higher values improve recall at the cost of memory usage and indexing speed.
     *
     * @return the maximum number of connections per node, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/performance/#hnsw-parameters">HNSW
     *      Parameters</a>
     */
    public Integer getMaxNodes() {
        return maxNodes;
    }

    /**
     * Gets the original dimensionality of vectors before dimensionality reduction.
     * <p>
     * When using the REDUCE option with VADD, this field indicates the original dimensionality of the vectors before they were
     * projected to a lower-dimensional space.
     *
     * @return the original dimensionality before reduction, or {@code null} if dimensionality reduction is not used
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/data-types/vector-sets/memory/#dimensionality-reduction">Dimensionality
     *      Reduction</a>
     */
    public Integer getProjectionInputDim() {
        return projectionInputDim;
    }

    /**
     * Gets the number of elements in the vector set that have attributes.
     * <p>
     * Attributes are JSON metadata associated with vector elements that can be used for filtering in similarity searches.
     *
     * @return the number of elements with attributes, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Filtered Search</a>
     */
    public Integer getAttributesCount() {
        return attributesCount;
    }

    /**
     * Gets the maximum level in the HNSW graph.
     * <p>
     * The HNSW algorithm organizes vectors in a hierarchical graph with multiple levels. This parameter indicates the highest
     * level in the graph structure.
     * <p>
     * Higher levels enable faster navigation through the graph during similarity searches.
     *
     * @return the maximum level in the HNSW graph, or {@code null} if not available
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/performance/#hnsw-parameters">HNSW
     *      Parameters</a>
     */
    public Integer getMaxLevel() {
        return maxLevel;
    }

    /**
     * Sets the dimensionality of the vectors in the vector set.
     *
     * @param value the number of dimensions for each vector in the vector set
     */
    public void setDimensionality(Integer value) {
        this.dimensionality = value;
    }

    /**
     * Sets the quantization type used for storing vectors in the vector set.
     *
     * @param value the quantization type (Q8, NOQUANT, or BIN)
     */
    public void setType(QuantizationType value) {
        this.type = value;
    }

    /**
     * Sets the number of elements (vectors) in the vector set.
     *
     * @param value the total count of vectors in the vector set
     */
    public void setSize(Integer value) {
        this.size = value;
    }

    /**
     * Sets the maximum node UID in the HNSW graph.
     *
     * @param value the maximum node UID in the HNSW graph
     */
    public void maxNodeUid(Integer value) {
        this.maxNodeUid = value;
    }

    /**
     * Sets the unique identifier of the vector set.
     *
     * @param value the unique identifier used by Redis to track the vector set
     */
    public void setvSetUid(Integer value) {
        this.vSetUid = value;
    }

    /**
     * Sets the maximum number of connections per node in the HNSW graph.
     * <p>
     * This parameter (also known as M) affects the memory usage and search performance of the vector set.
     *
     * @param value the maximum number of connections per node
     */
    public void setMaxNodes(Integer value) {
        this.maxNodes = value;
    }

    /**
     * Sets the original dimensionality of vectors before dimensionality reduction.
     * <p>
     * This is used when the REDUCE option is specified with VADD.
     *
     * @param value the original dimensionality before reduction
     */
    public void setProjectionInputDim(Integer value) {
        this.projectionInputDim = value;
    }

    /**
     * Sets the number of elements in the vector set that have attributes.
     *
     * @param value the number of elements with attributes
     */
    public void setAttributesCount(Integer value) {
        this.attributesCount = value;
    }

    /**
     * Sets the maximum level in the HNSW graph.
     * <p>
     * The HNSW algorithm organizes vectors in a hierarchical graph with multiple levels.
     *
     * @param value the maximum level in the HNSW graph
     */
    public void maxLevel(Integer value) {
        this.maxLevel = value;
    }

}
