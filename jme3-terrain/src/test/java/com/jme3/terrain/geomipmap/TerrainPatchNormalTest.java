package com.jme3.terrain.geomipmap;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TerrainPatchNormalTest {

    private TerrainPatch terrainPatch;
    private static final float EPSILON = 1e-5f;

    @BeforeEach
    public void setUp() {
        // Size is 3, so we need a heightmap array of 3x3 = 9 floats.
        float[] flatHeightMap = new float[9];
        
        // Instantiate using the full constructor to avoid the null heightmap issue
        terrainPatch = new TerrainPatch(
                "TestPatch", 
                3, 
                new Vector3f(1f, 1f, 1f), 
                flatHeightMap, 
                new Vector3f(0f, 0f, 0f)
        );
        
        // Test with non-uniform scale as before
        terrainPatch.setLocalScale(new Vector3f(1.0f, 2.0f, 1.0f)); 
    }

    @Test
    public void testAverageNormalsTangents_AllNeighborsPresent() {
        Vector3f root = new Vector3f(0, 5, 0);
        Vector3f top = new Vector3f(0, 6, -1);
        Vector3f bottom = new Vector3f(0, 4, 1);
        Vector3f left = new Vector3f(-1, 4, 0);
        Vector3f right = new Vector3f(1, 6, 0);

        verifyNormalsMatch(top, root, left, bottom, right);
    }

    @Test
    public void testAverageNormalsTangents_MissingTopLeftNeighbors() {
        Vector3f root = new Vector3f(0, 5, 0);
        Vector3f bottom = new Vector3f(0, 4, 1);
        Vector3f right = new Vector3f(1, 6, 0);

        // Simulating the corner of a map where top and left neighbors do not exist
        verifyNormalsMatch(null, root, null, bottom, right);
    }

    @Test
    public void testAverageNormalsTangents_FlatTerrain() {
        Vector3f root = new Vector3f(0, 0, 0);
        Vector3f top = new Vector3f(0, 0, -1);
        Vector3f bottom = new Vector3f(0, 0, 1);
        Vector3f left = new Vector3f(-1, 0, 0);
        Vector3f right = new Vector3f(1, 0, 0);

        verifyNormalsMatch(top, root, left, bottom, right);
    }

    private void verifyNormalsMatch(Vector3f top, Vector3f root, Vector3f left, Vector3f bottom, Vector3f right) {
        // Output vectors for the Refactored (Zero-Allocation) implementation
        Vector3f actualNormal = new Vector3f();
        Vector3f actualTangent = new Vector3f();
        Vector3f actualBinormal = new Vector3f();

        // Output vectors for the Legacy (Allocating) implementation
        Vector3f expectedNormal = new Vector3f();
        Vector3f expectedTangent = new Vector3f();
        Vector3f expectedBinormal = new Vector3f();

        // 1. Run Refactored Implementation (executes the actual TerrainPatch method)
        terrainPatch.averageNormalsTangents(
                top, root, left, bottom, right,
                actualNormal, actualTangent, actualBinormal
        );

        // 2. Run Legacy Implementation
        legacyAverageNormalsTangents(
                terrainPatch.getWorldScale(),
                top, root, left, bottom, right,
                expectedNormal, expectedTangent, expectedBinormal
        );

        // 3. Assert Equivalence
        assertVectorEquals("Normal vectors do not match!", expectedNormal, actualNormal);
        assertVectorEquals("Tangent vectors do not match!", expectedTangent, actualTangent);
        assertVectorEquals("Binormal vectors do not match!", expectedBinormal, actualBinormal);
    }

    /**
     * Exact copy of the old allocating logic to act as the ground truth.
     */
    private void legacyAverageNormalsTangents(
            Vector3f scale,
            Vector3f topPoint, Vector3f rootPoint, Vector3f leftPoint,
            Vector3f bottomPoint, Vector3f rightPoint,
            Vector3f normal, Vector3f tangent, Vector3f binormal) {

        Vector3f n1 = new Vector3f(0, 0, 0);
        if (topPoint != null && leftPoint != null) {
            n1.set(legacyCalculateNormal(topPoint.mult(scale), rootPoint.mult(scale), leftPoint.mult(scale)));
        }
        Vector3f n2 = new Vector3f(0, 0, 0);
        if (leftPoint != null && bottomPoint != null) {
            n2.set(legacyCalculateNormal(leftPoint.mult(scale), rootPoint.mult(scale), bottomPoint.mult(scale)));
        }
        Vector3f n3 = new Vector3f(0, 0, 0);
        if (rightPoint != null && bottomPoint != null) {
            n3.set(legacyCalculateNormal(bottomPoint.mult(scale), rootPoint.mult(scale), rightPoint.mult(scale)));
        }
        Vector3f n4 = new Vector3f(0, 0, 0);
        if (rightPoint != null && topPoint != null) {
            n4.set(legacyCalculateNormal(rightPoint.mult(scale), rootPoint.mult(scale), topPoint.mult(scale)));
        }

        normal.set(n1.add(n2).add(n3).add(n4).normalize());
        tangent.set(normal.cross(new Vector3f(0, 0, 1)).normalize());
        binormal.set(new Vector3f(1, 0, 0).cross(normal).normalize());
    }

    private Vector3f legacyCalculateNormal(Vector3f firstPoint, Vector3f rootPoint, Vector3f secondPoint) {
        Vector3f normal = new Vector3f();
        normal.set(firstPoint).subtractLocal(rootPoint)
                .crossLocal(secondPoint.subtract(rootPoint)).normalizeLocal();
        return normal;
    }

    private void assertVectorEquals(String message, Vector3f expected, Vector3f actual) {
        Assertions.assertEquals(expected.x, actual.x, EPSILON, message + " (X differs)");
        Assertions.assertEquals(expected.y, actual.y, EPSILON, message + " (Y differs)");
        Assertions.assertEquals(expected.z, actual.z, EPSILON, message + " (Z differs)");
    }
}