package ravensproject;

// Uncomment these lines to access image processing.
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
    enum SHAPES { SQUARE, CIRCLE, TRIANGLE, RECTANGLE, PENTAGON, HEXAGON, OCTAGON, DIAMOND, RIGHT_TRIANGLE, PAC_MAN, STAR, HEART, PLUS, UNKNOWN }
    enum SIZES   { VERY_SMALL, SMALL, MEDIUM, LARGE, VERY_LARGE, HUGE };
    enum VOCAB  { NOT_DEFINED,
                  BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT, BOTTOM, TOP,
                  LEFT_HALF, RIGHT_HALF, TOP_HALF, BOTTOM_HALF, YES, NO }
    enum TRANSLATIONS { UNKNOWN, UNCHANGED, DELETED, NEW, ENLARGE, SHRINK, MIRRORED, ROTATED,
                        MOVED, MOVED_UP, MOVED_DOWN, MOVED_LEFT, MOVED_RIGHT,
                        SHADED, UNSHADED, HALF_SHADED, HALF_UNSHADED, SHADE_CHANGED }
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
        if(object.getAttributes().containsKey("alignment"))
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
        if(position == nd.position) score++;
        if(fill == nd.fill) score++;
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
        if(n1.position != n2.position)
        {
            if(n1.position.name().contains("TOP") && !n2.position.name().contains("TOP")) translations.add(LexicalDatabase.TRANSLATIONS.MOVED_DOWN);
            if(n1.position.name().contains("BOTTOM") && !n2.position.name().contains("BOTTOM")) translations.add(LexicalDatabase.TRANSLATIONS.MOVED_UP);
            if(n1.position.name().contains("LEFT") && !n2.position.name().contains("LEFT")) translations.add(LexicalDatabase.TRANSLATIONS.MOVED_RIGHT);
            if(n1.position.name().contains("RIGHT") && !n2.position.name().contains("RIGHT")) translations.add(LexicalDatabase.TRANSLATIONS.MOVED_LEFT);
            translations.add(LexicalDatabase.TRANSLATIONS.MOVED);

        }
        if(n1.size.ordinal() > n2.size.ordinal()) translations.add(LexicalDatabase.TRANSLATIONS.SHRINK);
        if(n1.size.ordinal() < n2.size.ordinal()) translations.add(LexicalDatabase.TRANSLATIONS.ENLARGE);
        if(n1.angle != n2.angle) translations.add(LexicalDatabase.TRANSLATIONS.ROTATED);
        if(n1.fill != n2.fill)
        {
            if(n1.fill == LexicalDatabase.VOCAB.YES && n2.fill==LexicalDatabase.VOCAB.NO) translations.add(LexicalDatabase.TRANSLATIONS.UNSHADED);
            else if(n1.fill == LexicalDatabase.VOCAB.NO && n2.fill==LexicalDatabase.VOCAB.YES) translations.add(LexicalDatabase.TRANSLATIONS.SHADED);
            else if(n1.fill == LexicalDatabase.VOCAB.YES && n2.fill.name().contains("HALF")) translations.add(LexicalDatabase.TRANSLATIONS.HALF_UNSHADED);
            else if(n1.fill == LexicalDatabase.VOCAB.NO && n2.fill.name().contains("HALF")) translations.add(LexicalDatabase.TRANSLATIONS.HALF_SHADED);
            else if(n2.fill == LexicalDatabase.VOCAB.YES && n1.fill.name().contains("HALF")) translations.add(LexicalDatabase.TRANSLATIONS.HALF_UNSHADED);
            else if(n2.fill == LexicalDatabase.VOCAB.NO && n1.fill.name().contains("HALF")) translations.add(LexicalDatabase.TRANSLATIONS.HALF_SHADED);
            else translations.add(LexicalDatabase.TRANSLATIONS.SHADE_CHANGED);
        }
        if(n1.shape==n2.shape && n1.position==n2.position && n1.size==n2.size && n1.angle==n2.angle && n1.inside.size()==n2.inside.size() &&
                n1.above.size()==n2.above.size() && n1.left.size()==n2.left.size() && n1.right.size()==n2.right.size() && n1.fill==n2.fill)
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

        List<GraphNode> matchedList = new ArrayList<>();

        for(GraphNode nd : f1Nodes.values())
        {
            int score=0;
            GraphNode matchNode = null;
            for(GraphNode nd2 : f2Nodes.values())
            {
                int score2 = nd.GetSimilarityScore(nd2);
                if(score2 > score && !matchedList.contains(nd2)) {
                    score = score2;
                    matchNode = nd2;
                }
            }
            connections.add(new TranslationConnection(nd,matchNode));
            matchedList.add(matchNode);
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
    public TranslationConnection GetConnectionFromNode(GraphNode nd, int which)
    {
        for(TranslationConnection connection : connections)
        {
            if(which == 1)
                if(nd==connection.Node1)
                    return connection;
            if(which == 2)
                if(nd==connection.Node2)
                    return connection;
        }
        if(connections.size()==1) return connections.get(0);
        for(TranslationConnection connection : connections)
        {
            if(which == 1)
                if(nd==null)
                    return connection;
            if(which == 2)
                if(nd==null)
                    return connection;
        }

        return null;
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

    public HashMap<String, RPM_Graph> generateNets()
    {
        Set<String> keys = ravensfigures.keySet();
        HashMap<String, RPM_Graph> nets = new HashMap<>();

        for (String k : keys)
        {
            if(Character.isDigit(k.charAt(0)))
            {
                nets.put(k,new RPM_Graph(ravensfigures,problemType,lexicalDatabase,k));
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
    public StatisticalAnalyzer(HashMap<String, RPM_Graph> rpms, LexicalDatabase db)
    {
        Rpms = rpms;
        lexicalDatabase = db;
    }
    public HashMap<String,Integer> GetScores_2x2()
    {
        HashMap<String,Integer> Scores = new HashMap<>();

        for(String key : Rpms.keySet())
        {
            int score = 0;
            int numDeleted1=0;
            int numDeleted2=0;
            Scores.put(key,score);
            TranslationGraph ab = Rpms.get(key).translationGraph.get("AB");
            TranslationGraph ac = Rpms.get(key).translationGraph.get("AC");
            TranslationGraph cn = Rpms.get(key).translationGraph.get("C"+key);
            TranslationGraph bn = Rpms.get(key).translationGraph.get("B"+key);

            if(ab.connections.size() == cn.connections.size()) score++;
            if(ac.connections.size() == bn.connections.size()) score++;

            for(TranslationConnection cn1 : bn.connections)
            {
                for(TranslationConnection cn2 : ac.connections)
                {
                    if(cn2.translations.containsAll(cn1.translations)) score+=cn1.translations.size();
                }
            }
            for(TranslationConnection cn1 : cn.connections)
            {
                for(TranslationConnection cn2 : ab.connections)
                {
                    if(cn2.translations.containsAll(cn1.translations)) score+=cn1.translations.size();
                }
            }
            for(TranslationConnection cn1 : bn.connections)
                if(cn1.translations.contains(LexicalDatabase.TRANSLATIONS.DELETED)) numDeleted1++;
            for(TranslationConnection cn1 : ac.connections)
                if(cn1.translations.contains(LexicalDatabase.TRANSLATIONS.DELETED)) numDeleted2++;

            if(numDeleted1==numDeleted2) score+=numDeleted1;
            numDeleted1=0;
            numDeleted2=0;

            for(TranslationConnection cn1 : ab.connections)
                if(cn1.translations.contains(LexicalDatabase.TRANSLATIONS.DELETED)) numDeleted1++;
            for(TranslationConnection cn1 : cn.connections)
                if(cn1.translations.contains(LexicalDatabase.TRANSLATIONS.DELETED)) numDeleted2++;

            if(numDeleted1==numDeleted2) score+=numDeleted1;
            numDeleted1=0;
            numDeleted2=0;

            for(TranslationConnection bn_connection : bn.connections) {
                TranslationConnection ab_connection = ab.GetConnectionFromNode(bn_connection.Node1, 2);
                TranslationConnection cn_connection = cn.GetConnectionFromNode(bn_connection.Node2, 2);
                TranslationConnection ac_connection = null;
                if(ab_connection!=null)
                    ac_connection = ac.GetConnectionFromNode(ab_connection.Node1, 1);
                else if(cn_connection != null)
                    ac_connection = ac.GetConnectionFromNode(cn_connection.Node1, 2);
                if(ab_connection==null && cn_connection!=null && ac_connection!=null)
                {
                    ab_connection = ab.GetConnectionFromNode(ac_connection.Node1,1);
                }
                if(cn_connection==null && ab_connection!=null && ac_connection!=null)
                {
                    cn_connection = cn.GetConnectionFromNode(ac_connection.Node2,1);
                }


                if (ab_connection != null && cn_connection != null) {
                    if (ab_connection.Node1 != null && ab_connection.Node2 != null && cn_connection.Node1 != null && cn_connection.Node2 != null) {
                        if ((ab_connection.Node1.inside.size() - ab_connection.Node2.inside.size()) == (cn_connection.Node1.inside.size() - cn_connection.Node2.inside.size()))
                            score++;
                        if ((ab_connection.Node1.above.size() - ab_connection.Node2.above.size()) == (cn_connection.Node1.above.size() - cn_connection.Node2.above.size()))
                            score++;
                        if ((ab_connection.Node1.left.size() - ab_connection.Node2.left.size()) == (cn_connection.Node1.left.size() - cn_connection.Node2.left.size()))
                            score++;
                        if ((ab_connection.Node1.right.size() - ab_connection.Node2.right.size()) == (cn_connection.Node1.right.size() - cn_connection.Node2.right.size()))
                            score++;
                    }
                    for (LexicalDatabase.TRANSLATIONS tr : ab_connection.translations) {
                        if (cn_connection.translations.contains(tr)) {
                            score++;
                            if (tr == LexicalDatabase.TRANSLATIONS.ROTATED) {
                                double angleDiff1 = (cn_connection.Node1.angle - cn_connection.Node2.angle);
                                double angleDiff2 = (ab_connection.Node1.angle - ab_connection.Node2.angle);
                                if(angleDiff1<0) angleDiff1+=360;
                                if(angleDiff2<0) angleDiff2+=360;
                                if((angleDiff1==90 ) && (angleDiff2==270))
                                    score+=2; //Mirrored
                                else if((angleDiff1==270 ) && (angleDiff2==90))
                                    score+=2; //mirrored
                                else if (angleDiff1 == angleDiff2)
                                    score++;
                            }
                            if(tr == LexicalDatabase.TRANSLATIONS.NEW)
                                if(cn_connection.Node2.shape == ab_connection.Node2.shape)
                                    score++;
                        }
                    }
                }
                if (bn_connection != null && ac_connection != null) {
                    if (bn_connection.Node1 != null && bn_connection.Node2 != null && ac_connection.Node1 != null && ac_connection.Node2 != null) {
                        if ((bn_connection.Node1.inside.size() - bn_connection.Node2.inside.size()) == (ac_connection.Node1.inside.size() - ac_connection.Node2.inside.size()))
                            score++;
                        if ((bn_connection.Node1.above.size() - bn_connection.Node2.above.size()) == (ac_connection.Node1.above.size() - ac_connection.Node2.above.size()))
                            score++;
                        if ((bn_connection.Node1.left.size() - bn_connection.Node2.left.size()) == (ac_connection.Node1.left.size() - ac_connection.Node2.left.size()))
                            score++;
                        if ((bn_connection.Node1.right.size() - bn_connection.Node2.right.size()) == (ac_connection.Node1.right.size() - ac_connection.Node2.right.size()))
                            score++;
                    }

                    for (LexicalDatabase.TRANSLATIONS tr : ac_connection.translations) {
                        if (bn_connection.translations.contains(tr)) {
                            score++;
                            if (tr == LexicalDatabase.TRANSLATIONS.ROTATED) {
                                double angleDiff1 = ac_connection.Node1.angle - ac_connection.Node2.angle;
                                double angleDiff2 = bn_connection.Node1.angle - bn_connection.Node2.angle;
                                if(angleDiff1<0) angleDiff1+=360;
                                if(angleDiff2<0) angleDiff2+=360;
                                if((angleDiff1==90 ) && (angleDiff2==270))
                                    score+=2; //Mirrored
                                else if((angleDiff1==270) && (angleDiff2==90))
                                    score+=2; //mirrored
                                else if (angleDiff1 == angleDiff2)
                                    score++;
                            }
                        }
                    }
                }
            }
            Scores.put(key,score);
        }
        return Scores;
    }
    public HashMap<String, RPM_Graph> Rpms;
    public LexicalDatabase lexicalDatabase;
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
        HashMap<String, RPM_Graph> rpms = generator.generateNets();
        StatisticalAnalyzer analyzer = new StatisticalAnalyzer(rpms,lexicalDatabase);
        if(problem.getProblemType().contentEquals("2x2"))
        {
            HashMap<String, Integer> scores = analyzer.GetScores_2x2();
            String maxKey = new String();
            maxKey = "-1";
            Integer maxVal=-1;
            for(String key : scores.keySet())
            {
                if(scores.get(key)>maxVal)
                {
                    maxKey=key;
                    maxVal = scores.get(key);
                }
            }
            int val = Integer.parseInt(maxKey);
            return val;
        }

        return -1;
    }

    private LexicalDatabase lexicalDatabase;
}
