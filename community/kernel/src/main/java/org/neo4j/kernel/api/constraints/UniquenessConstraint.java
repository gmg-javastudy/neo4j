/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.kernel.api.constraints;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.StatementTokenNameLookup;
import org.neo4j.kernel.api.TokenNameLookup;
import org.neo4j.kernel.impl.coreapi.schema.InternalSchemaActions;
import org.neo4j.kernel.impl.coreapi.schema.UniquenessConstraintDefinition;

public class UniquenessConstraint extends NodePropertyConstraint
{
    public UniquenessConstraint( int labelId, int propertyKeyId )
    {
        super( labelId, propertyKeyId );
    }

    @Override
    public void added( ChangeVisitor visitor )
    {
        visitor.visitAddedUniquePropertyConstraint( this );
    }

    @Override
    public void removed( ChangeVisitor visitor )
    {
        visitor.visitRemovedUniquePropertyConstraint( this );
    }

    @Override
    public ConstraintDefinition asConstraintDefinition( InternalSchemaActions schemaActions, ReadOperations readOps )
    {
        StatementTokenNameLookup lookup = new StatementTokenNameLookup( readOps );
        return new UniquenessConstraintDefinition( schemaActions,
                DynamicLabel.label( lookup.labelGetName( labelId ) ),
                lookup.propertyKeyGetName( propertyKeyId ) );
    }

    @Override
    public ConstraintType type()
    {
        return ConstraintType.UNIQUENESS;
    }

    @Override
    public String userDescription( TokenNameLookup tokenNameLookup )
    {
        String labelName = tokenNameLookup.labelGetName( labelId );
        String boundIdentifier = labelName.toLowerCase();
        return String.format( "CONSTRAINT ON ( %s:%s ) ASSERT %s.%s IS UNIQUE",
                boundIdentifier, labelName, boundIdentifier, tokenNameLookup.propertyKeyGetName( propertyKeyId ) );
    }

    @Override
    public String toString()
    {
        return String.format( "CONSTRAINT ON ( n:label[%s] ) ASSERT n.property[%s] IS UNIQUE",
                labelId, propertyKeyId );
    }
}
