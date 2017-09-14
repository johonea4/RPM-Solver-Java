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
    enum SHAPES { SQUARE, CIRCLE, TRIANGLE, RECTANGLE, PENTAGON, HEXAGON, OCTAGON, DIAMOND, RIGHT_TRIANGLE, PAC_MAN, STAR, HEART, UNKNOWN }
    enum SIZES   { VERY_SMALL, SMALL, MEDIUM, LARGE, VERY_LARGE, HUGE };
    enum VOCAB  { NOT_DEFINED,
                  BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT, BOTTOM, TOP,
                  LEFT_HALF, RIGHT_HALF, TOP_HALF, BOTTOM_HALF, YES, NO }
    enum TRANSLATIONS { UNKNOWN, UNCHANGED, DELETED, MOVED, ENLARGE, SHRINK, ROTATED, MIRRORED, NEW }
    public LexicalDatabase()
    {
        database = new HashMap<>();
        for( SHAPES v : SHAPES.values()) database.put(v.name(),v);
        for( SIZES v : SIZES.values()) database.put(v.name(),v);
        for( VOCAB v : VOCAB.values()) database.put(v.name(),v);
        for( TRANSLATIONS v : TRANSLATIONS.values()) database.put(v.name(),v);
        for( KEYS v : KEYS.values()) database.put(v.name(),v);
        for( RELATIONSHIPS v : RELATIONSHIPS.values()) database.put(v.name(),v);
    }

    public Object getValue(String s)
    {
        if(s == null || s.isEmpty()) return VOCAB.NOT_DEFINED;
        s = s.replaceAll(" ", "_");
        s = s.replaceAll("-", "_");
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
        above = new ArrayList<>();
        inside = new ArrayList<>();
        left = new ArrayList<>();
        right = new ArrayList<>();
        object = obj;
        objectName = obj.getName();
        if(object.getAttributes().containsKey(LexicalDatabase.KEYS.ALIGNMENT.name()))
            position = (LexicalDatabase.VOCAB)db.getValue(obj.getAttributes().get("alignment"));
        else position = LexicalDatabase.VOCAB.NOT_DEFINED;
        if(object.getAttributes().containsKey("shape"))
            shape = (LexicalDatabase.SHAPES)db.getValue(obj.getAttributes().get("shape"));
        else
            shape = LexicalDatabase.SHAPES.UNKNOWN;
        if(object.getAttributes().containsKey("size"))
            size = (LexicalDatabase.SIZES)db.getValue(obj.getAttributes().get("size"));
        else
            size = LexicalDatabase.SIZES.MEDIUM;
        if(object.getAttributes().containsKey("fill"))
            fill = (LexicalDatabase.VOCAB)db.getValue(obj.getAttributes().get("fill"));
        else
            fill = (LexicalDatabase.VOCAB.NOT_DEFINED);
        if(object.getAttributes().containsKey("angle"))
            angle = (double) ( db.getValue(obj.getAttributes().get("angle")));
        else
            angle = 0;
    }
    public void GetRelationships(HashMap<String,GraphNode> objects)
    {
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.ABOVE.name().toLowerCase()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.ABOVE.name().toLowerCase()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) above.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.INSIDE.name().toLowerCase()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.INSIDE.name().toLowerCase()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) inside.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.LEFT.name().toLowerCase()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.LEFT.name().toLowerCase()).split(","));
            for(String obj : list) if(objects.containsKey(obj)) left.add(objects.get(obj));
        }
        if(object.getAttributes().containsKey(LexicalDatabase.RELATIONSHIPS.RIGHT.name().toLowerCase()))
        {
            List<String> list = Arrays.asList(object.getAttributes().get(LexicalDatabase.RELATIONSHIPS.RIGHT.name().toLowerCase()).split(","));
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

    public int GetSimilarityScore(GraphNode nd)
    {
        int score=0;
        if(shape == nd.shape) score+=10;
        if(size == nd.size) score+=5;
        return score;
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
        Nodes = new HashMap<>();

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
    TranslationConnection(GraphNode n1, GraphNode n2)
    {
        Node1 = n1;
        Node2 = n2;
        translations = new ArrayList<LexicalDatabase.TRANSLATIONS>();

        if(n1 == null) { translations.add(LexicalDatabase.TRANSLATIONS.NEW); return; }
        if(n2 == null) { translations.add(LexicalDatabase.TRANSLATIONS.DELETED); return; }
        if(n1.shape != n2.shape) translations.add(LexicalDatabase.TRANSLATIONS.DELETED);
        if(n2.shape != n1.shape) translations.add(LexicalDatabase.TRANSLATIONS.NEW);
        if(n1.position != n2.position) translations.add(LexicalDatabase.TRANSLATIONS.MOVED);
        if(n1.size.ordinal() > n2.size.ordinal()) translations.add(LexicalDatabase.TRANSLATIONS.SHRINK);
        if(n1.size.ordinal() < n2.size.ordinal()) translations.add(LexicalDatabase.TRANSLATIONS.ENLARGE);
        if(n1.angle != n2.angle) translations.add(LexicalDatabase.TRANSLATIONS.ROTATED);
        if(n1.shape==n2.shape && n1.position==n2.position && n1.size==n2.size && n1.angle==n2.angle && n1.inside.size()==n2.inside.size() &&
                n1.above.size()==n2.above.size() && n1.left.size()==n2.left.size() && n1.right.size()==n2.right.size())
            translations.add(LexicalDatabase.TRANSLATIONS.UNCHANGED);
        if(translations.size()<=0) translations.add(LexicalDatabase.TRANSLATIONS.UNKNOWN);
    }
    public GraphNode Node1;
    public GraphNode Node2;
    public List<LexicalDatabase.TRANSLATIONS> translations;
}

class TranslationGraph
{
    public TranslationGraph(RavensFigure f1, RavensFigure f2, LexicalDatabase db)
    {
        figure1 = new FigureGraph(f1, db);
        figure2 = new FigureGraph(f2, db);
        connections = new ArrayList<TranslationConnection>();

        //--- Find similar nodes across figures
        HashMap<String, GraphNode> f1Nodes = figure1.Nodes;
        HashMap<String, GraphNode> f2Nodes = figure2.Nodes;

        for(GraphNode nd : f1Nodes.values())
        {
            int score=0;
            GraphNode matchNode = null;
            for(GraphNode nd2 : f2Nodes.values())
            {
                int score2 = nd.GetSimilarityScore(nd2);
                if(score2 > score) {
                    score = score2;
                    matchNode = nd2;
                }
            }
            connections.add(new TranslationConnection(nd,matchNode));
        }
        for(GraphNode nd : f2Nodes.values())
        {
            int score=0;
            for(GraphNode nd1 : f1Nodes.values())
            {
                int score2 = nd.GetSimilarityScore(nd1);
                if(score2 > score)
                {
                    score = score2;
                    break;
                }
            }
            if(score==0) connections.add(new TranslationConnection(null, nd));
        }
    }
    public FigureGraph figure1;
    public FigureGraph figure2;
    public List<TranslationConnection> connections;
}

class RPM_Graph
{
    //---- 2x2 will result in A->B, C->N, A->C, B->N
    //---- 3x3 will result in A->B,B->C, D->E,E->F, G->H,H->N
    //----                    A->D,D->G, B->E,E->H, C->F,F->N
    RPM_Graph(HashMap<String, RavensFigure> figures, int ptype, LexicalDatabase db, String N)
    {
        ProblemType = ptype;
        translationGraph = new HashMap<>();
        if(ptype == 0)
        {
            translationGraph.put("AB",new TranslationGraph(figures.get("A"),figures.get("B"),db));
            translationGraph.put("B" + N,new TranslationGraph(figures.get("B"),figures.get(N),db));
            translationGraph.put("AC",new TranslationGraph(figures.get("A"),figures.get("C"),db));
            translationGraph.put("C" + N,new TranslationGraph(figures.get("C"),figures.get(N),db));
        }
        if(ptype == 1)
        {
            translationGraph.put("AB",new TranslationGraph(figures.get("A"),figures.get("B"),db));
            translationGraph.put("BC",new TranslationGraph(figures.get("B"),figures.get("C"),db));
            translationGraph.put("DE",new TranslationGraph(figures.get("D"),figures.get("E"),db));
            translationGraph.put("EF",new TranslationGraph(figures.get("E"),figures.get("F"),db));
            translationGraph.put("GH",new TranslationGraph(figures.get("G"),figures.get("H"),db));
            translationGraph.put("H"+N,new TranslationGraph(figures.get("H"),figures.get(N),db));
            translationGraph.put("AD",new TranslationGraph(figures.get("A"),figures.get("D"),db));
            translationGraph.put("DG",new TranslationGraph(figures.get("D"),figures.get("G"),db));
            translationGraph.put("BE",new TranslationGraph(figures.get("B"),figures.get("E"),db));
            translationGraph.put("EH",new TranslationGraph(figures.get("E"),figures.get("H"),db));
            translationGraph.put("CF",new TranslationGraph(figures.get("C"),figures.get("F"),db));
            translationGraph.put("F" + N,new TranslationGraph(figures.get("F"),figures.get(N),db));

        }

    }
    HashMap<String, TranslationGraph> translationGraph;
    int ProblemType;
}

class SemanticNetworkGenerator
{
    public SemanticNetworkGenerator(LexicalDatabase ld, HashMap<String, RavensFigure> rf, int ptype)
    {
        ravensfigures = rf;
        lexicalDatabase = ld;
        problemType = ptype;
    }

    public List<RPM_Graph> generateNets()
    {
        Set<String> keys = ravensfigures.keySet();
        List<RPM_Graph> nets = new ArrayList<RPM_Graph>();

        for (String k : keys)
        {
            if(Character.isDigit(k.charAt(0)))
            {
                nets.add(new RPM_Graph(ravensfigures,problemType,lexicalDatabase,k));
            }
        }
        return nets;
    }

    private HashMap<String, RavensFigure> ravensfigures;
    private LexicalDatabase lexicalDatabase;
    private int problemType;
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


        HashMap<String, RavensFigure> figures = problem.getFigures();

        SemanticNetworkGenerator generator = new SemanticNetworkGenerator(lexicalDatabase, figures, problem.getProblemType().contentEquals("2x2") ? 0 : 1);
        List<RPM_Graph> rpms = generator.generateNets();

        return -1;
    }

    private LexicalDatabase lexicalDatabase;
}
