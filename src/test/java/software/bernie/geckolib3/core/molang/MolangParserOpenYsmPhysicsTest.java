package software.bernie.geckolib3.core.molang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MolangParserOpenYsmPhysicsTest {

    @Test
    void parsesOpenYsmPhysicsFunctionsWithStringKeys() throws Exception {
        MolangParser parser = new MolangParser();

        double value = parser
            .parseExpression("v.physics_test=ysm.second_order('胸部垂直', 4, 3, 0.3, 0);v.physics_test")
            .get();

        assertEquals(4.0D, value, 0.0001D);
    }

    @Test
    void rewritesOpenYsmBoneVectorAccessors() throws Exception {
        MolangParser parser = new MolangParser();

        double value = parser.parseExpression("ysm.bone_rot('BackHairA1').x + ysm.bone_pos('BackHairA1').z")
            .get();

        assertEquals(0.0D, value, 0.0001D);
    }

    @Test
    void splitsTimelineStatementsOutsideQuotedStrings() throws Exception {
        List<String> statements = MolangParser.splitStatements("v.a='x;y';v.b=2");

        assertEquals(2, statements.size());
        assertEquals("v.a='x;y'", statements.get(0));
        assertEquals("v.b=2", statements.get(1));
    }
}
