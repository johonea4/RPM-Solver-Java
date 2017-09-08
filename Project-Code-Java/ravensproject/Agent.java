package ravensproject;

// Uncomment these lines to access image processing.
import java.awt.Image;
import java.io.Console;
import java.io.File;
import java.sql.Array;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;

class LexicalDatabase
{
    public LexicalDatabase()
    {
        database.clear();
    }
    public int getValue(String val)
    {
        if(!database.containsKey(val)) database.put(val,database.size()+1);
        return database.get(val);
    }

    private HashMap<String, Integer> database;
}

class GraphNode
{
    public String objectName;
    public RavensObject object;
    public HashMap<Integer,Integer> attributes;
    public int MatchScore(GraphNode node)
    {
        int score = 0;
        if(this.attributes.size() == node.attributes.size()) score++;
        for(Integer i : this.attributes.keySet())
        {
            if(node.attributes.containsKey(i))
            {
                score++;
                if(node.attributes.get(i) == this.attributes.get(i)) score++;
            }
        }
        return score;
    }
}

class NodeRelationship
{
    public GraphNode node1;
    public GraphNode node2;
    public int relationship;
}

class FigureGraph
{
    public String figureName;
    public RavensFigure figure;
    public ArrayList<NodeRelationship> relationships;

}

class TranslationConnection
{
    public GraphNode figure1Node;
    public GraphNode figure2Node;
    public int translation;
}

class FigureTranslation
{
    public String f1Name;
    public String f2Name;
    public FigureGraph figure1;
    public FigureGraph figure2;
    public ArrayList<TranslationConnection> translations;
}

class RPM_Graph
{
    //---- 2x2 will result in A->B, C->N, A->C, B->N
    //---- 3x3 will result in A->B,B->C, D->E,E->F, G->H,H->N
    //----                    A->D,D->G, B->E,E->H, C->F,F->N
    public int problemType;
    public ArrayList<FigureTranslation> rpm;
}

class SemanticNetworkGenerator
{
    public SemanticNetworkGenerator(LexicalDatabase ld, HashMap<String, RavensFigure> rf, int ptype)
    {
        ravensfigures = rf;
        lexicalDatabase = ld;
        problemType = ptype;
    }

    public ArrayList<RPM_Graph> generateNets()
    {
        if(problemType==0) return generateNets_2x2();
        else return generateNets_3x3();
    }

    private HashMap<String, RavensFigure> ravensfigures;
    private LexicalDatabase lexicalDatabase;
    private int problemType;

    private ArrayList<RPM_Graph> generateNets_2x2()
    {
        //---Alright, First each figure needs relationships built

        //---Next Find matching figures between figures

        //---Next Find the translations between the figures

        //---Return a list of all graphs
    }

    private ArrayList<RPM_Graph> generateNets_3x3()
    {
        //---Alright, First each figure needs relationships built

        //---Next Find matching figures between figures

        //---Next Find the translations between the figures

        //---Return a list of all graphs
    }

}

class StatisticalAnalyzer
{

}

class NetworkTester
{

}

/**
 * Your Agent for solving Raven's Progressive Matrices. You MUST modify this
 * file.
 * 
 * You may also create and submit new files in addition to modifying this file.
 * 
 * Make sure your file retains methods with the signatures:
 * public Agent()
 * public char Solve(RavensProblem problem)
 * 
 * These methods will be necessary for the project's main method to run.
 * 
 */
public class Agent {
    /**
     * The default constructor for your Agent. Make sure to execute any
     * processing necessary before your Agent starts solving problems here.
     * 
     * Do not add any variables to this signature; they will not be used by
     * main().
     * 
     */
    public Agent() {
        lexicalDatabase = new LexicalDatabase();
    }
    /**
     * The primary method for solving incoming Raven's Progressive Matrices.
     * For each problem, your Agent's Solve() method will be called. At the
     * conclusion of Solve(), your Agent should return an int representing its
     * answer to the question: 1, 2, 3, 4, 5, or 6. Strings of these ints 
     * are also the Names of the individual RavensFigures, obtained through
     * RavensFigure.getName(). Return a negative number to skip a problem.
     * 
     * Make sure to return your answer *as an integer* at the end of Solve().
     * Returning your answer as a string may cause your program to crash.
     * @param problem the RavensProblem your agent should solve
     * @return your Agent's answer to this problem
     */
    public int Solve(RavensProblem problem) {

        if(problem.hasVisual() && !problem.hasVerbal()) return -1;

        System.console().printf("=========================================\n");
        System.console().printf("Starting Problem: %s\n", problem.getName());
        System.console().printf("Problem Type: %s\n",problem.getProblemType());
        System.console().printf("Num Figures: %d\n", problem.getFigures().size());
        System.console().printf("=========================================\n");

        HashMap<String, RavensFigure> figures = problem.getFigures();

        SemanticNetworkGenerator generator = new SemanticNetworkGenerator(lexicalDatabase, figures, problem.getProblemType()== "2x2" ? 0 : 1);



        System.console().printf("=========================================\n");

        return -1;
    }

    private LexicalDatabase lexicalDatabase;
}
