package ravensproject;

// Uncomment these lines to access image processing.
import com.sun.deploy.util.StringUtils;
import jdk.nashorn.internal.codegen.types.NumericType;

import java.awt.Image;
import java.io.Console;
import java.io.File;
import java.sql.Array;
import java.sql.Struct;
import java.util.*;
import java.lang.Object;
import javax.imageio.ImageIO;

class LexicalDatabase
{
    enum KEYS { SHAPE, ALIGNMENT, FILL, SIZE, ANGLE }
    enum RELATIONSHIPS   { UNKNOWN, ABOVE, BELOW, INSIDE, OUTSIDE, LEFT, RIGHT, FILL, SIZE, ANGLE, SELF }
    enum SHAPES { SQUARE, CIRCLE, TRIANGLE, RECTANGLE, PENTAGON, HEXAGON, OCTAGON, DIAMOND, RIGHT_TRIANGLE }
    enum SIZES   { VERY_SMALL, SMALL, MEDIUM, LARGE, VERY_LARGE, HUGE };
    enum VOCAB  { NOT_DEFINED,
                  BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT, BOTTOM, TOP,
                  LEFT_HALF, RIGHT_HALF, TOP_HALF, BOTTOM_HALF, YES, NO }
    enum TRANSLATIONS { UNKNOWN, UNCHANGED, DELETED, MOVED, ENLARGE, SHRINK, SUBTRACTED, ROTATED, MIRRORED, NEW }
    public LexicalDatabase()
    {
        database.clear();
        for( SHAPES v : SHAPES.values()) database.put(v.name(),v);
        for( SIZES v : SIZES.values()) database.put(v.name(),v);
        for( VOCAB v : VOCAB.values()) database.put(v.name(),v);
        for( TRANSLATIONS v : TRANSLATIONS.values()) database.put(v.name(),v);
        for( KEYS v : KEYS.values()) database.put(v.name(),v);
        for( RELATIONSHIPS v : RELATIONSHIPS.values()) database.put(v.name(),v);
    }

    public Object getValue(String s)
    {
        if(s.isEmpty()) return VOCAB.NOT_DEFINED;
        s = s.replaceAll(" ", "_");
        s = s.toUpperCase();
        if(database.containsKey(s)) return database.get(s);
        boolean isNumber=true;
        double n = 0;
        try { n = Double.parseDouble(s); }
        catch (NumberFormatException e){  isNumber=false; }

        if(isNumber) return n;

        database.put(s,database.size()+1);
        return database.get(s);
    }

    HashMap<String,Object> database;
}

class GraphNode
{
    public GraphNode(RavensObject obj, LexicalDatabase db)
    {
        object = obj;
        objectName = obj.getName();
        position = (LexicalDatabase.VOCAB)db.getValue(obj.getAttributes().get("alignment"));
        shape = (LexicalDatabase.SHAPES)db.getValue(obj.getAttributes().get("shape"));
        size = (LexicalDatabase.SIZES)db.getValue(obj.getAttributes().get("size"));
        fill = (LexicalDatabase.VOCAB)db.getValue(obj.getAttributes().get("fill"));
        angle = (double)db.getValue(obj.getAttributes().get("angle"));
    }
    public void GetRelationships(HashMap<String,GraphNode> objects)
    {
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.ABOVE.name()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.ABOVE.name()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) above.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.INSIDE.name()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.INSIDE.name()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) inside.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.LEFT.name()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.LEFT.name()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) left.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.RIGHT.name()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.RIGHT.name()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) right.add(objects.get(obj));
        }
        if(position != LexicalDatabase.VOCAB.NOT_DEFINED)
        {
            if(position.name().contains("TOP"))
                for(GraphNode nd : objects.values())
                    if(nd.position != LexicalDatabase.VOCAB.NOT_DEFINED && !nd.position.name().contains("TOP")) above.add(nd);
            if(position.name().contains("LEFT"))
                for(GraphNode nd : objects.values())
                    if(nd.position != LexicalDatabase.VOCAB.NOT_DEFINED && !nd.position.name().contains("LEFT")) above.add(nd);
            if(position.name().contains("RIGHT"))
                for(GraphNode nd : objects.values())
                    if(nd.position != LexicalDatabase.VOCAB.NOT_DEFINED && !nd.position.name().contains("RIGHT")) above.add(nd);
            for(GraphNode nd : objects.values())
                if(nd.position != LexicalDatabase.VOCAB.NOT_DEFINED && nd.position == position)
                    if(nd.size.ordinal() > size.ordinal())
                        inside.add(nd);
        }
    }

    public String objectName;
    public RavensObject object;
    public LexicalDatabase.VOCAB position;
    public LexicalDatabase.SHAPES shape;
    public LexicalDatabase.SIZES size;
    public LexicalDatabase.VOCAB fill;
    public double angle;
    public List<GraphNode> inside;
    public List<GraphNode> above;
    public List<GraphNode> left;
    public List<GraphNode> right;
}

class FigureGraph
{
    public FigureGraph(RavensFigure f, LexicalDatabase db)
    {
        figureName = f.getName();
        figure = f;

        HashMap<String, RavensObject> nodelist = f.getObjects();
        for(RavensObject n : nodelist.values())
            Nodes.put(n.getName(), new GraphNode(n,db));
        for(GraphNode n1 : Nodes.values())
            n1.GetRelationships(Nodes);
    }
    public String figureName;
    public RavensFigure figure;
    public HashMap<String,GraphNode> Nodes;

}
//----Here Down-----> Still Working
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

    private FigureGraph BuildFigureRelationships(RavensFigure f)
    {
        HashMap<String, RavensObject> objs = f.getObjects();
        ArrayList<GraphNode> nodes = new ArrayList<GraphNode>();
        for(String key : objs.keySet())
        {
            nodes.add(new GraphNode(objs.get(key),lexicalDatabase));
        }

        for( GraphNode nd : nodes)
        {
            for( GraphNode nd2 : nodes)
            {
                if(nd == nd2) continue;
            }
        }
    }
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
