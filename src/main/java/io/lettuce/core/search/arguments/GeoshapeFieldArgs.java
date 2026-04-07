/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

import java.util.Optional;

import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Field arguments for GEOSHAPE fields in a RediSearch index.
 * <p>
 * Geoshape fields provide more advanced functionality than GEO fields. You can use them to represent locations as points but
 * also to define shapes and query the interactions between points and shapes (for example, to find all points that are
 * contained within an enclosing shape). You can also choose between geographical coordinates (on the surface of a sphere) or
 * standard Cartesian coordinates.
 *
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#geoshape-fields">Geoshape
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GeoshapeFieldArgs extends FieldArgs {

    /**
     * Coordinate system for geoshape fields.
     */
    public enum CoordinateSystem {
        /**
         * Cartesian (planar) coordinates.
         */
        FLAT,
        /**
         * Spherical (geographical) coordinates. This is the default option.
         */
        SPHERICAL
    }

    private Optional<CoordinateSystem> coordinateSystem = Optional.empty();

    /**
     * Create a new {@link GeoshapeFieldArgs} using the builder pattern.
     * 
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getFieldType() {
        return "GEOSHAPE";
    }

    /**
     * Get the coordinate system.
     *
     * @return the coordinate system
     */
    public Optional<CoordinateSystem> getCoordinateSystem() {
        return coordinateSystem;
    }

    @Override
    protected void buildTypeSpecificArgs(CommandArgs<?, ?> args) {
        coordinateSystem.ifPresent(cs -> {
            switch (cs) {
                case FLAT:
                    args.add(FLAT);
                    break;
                case SPHERICAL:
                    args.add(SPHERICAL);
                    break;
            }
        });
    }

    /**
     * Builder for {@link GeoshapeFieldArgs}.
     * 
     */
    public static class Builder extends FieldArgs.Builder<GeoshapeFieldArgs, Builder> {

        public Builder() {
            super(new GeoshapeFieldArgs());
        }

        /**
         * Set the coordinate system for the geoshape field.
         * 
         * @param coordinateSystem the coordinate system
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder coordinateSystem(CoordinateSystem coordinateSystem) {
            instance.coordinateSystem = Optional.of(coordinateSystem);
            return self();
        }

        /**
         * Use Cartesian (planar) coordinates.
         * 
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder flat() {
            return coordinateSystem(CoordinateSystem.FLAT);
        }

        /**
         * Use spherical (geographical) coordinates. This is the default option.
         * 
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder spherical() {
            return coordinateSystem(CoordinateSystem.SPHERICAL);
        }

    }

}
