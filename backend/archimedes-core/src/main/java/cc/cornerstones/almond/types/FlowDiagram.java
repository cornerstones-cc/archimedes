package cc.cornerstones.almond.types;

import lombok.Data;

import java.util.List;

@Data
public class FlowDiagram {
    private List<ImgNode> nodes;
    private List<ImgEdge> edges;

    public static final String NODE_TYPE_TASK = "TASK";
    public static final String NODE_TYPE_DATA = "DATA";
    public static final String NODE_TYPE_EVENT = "EVENT";

    public static final String EDGE_TYPE_SIGNALING = "SOLID";
    public static final String EDGE_TYPE_TRAFFIC = "DOTTED";

    public static final String NODE_TYPE_INPUT_ID_PREFIX = "i_";
    public static final String NODE_TYPE_OUTPUT_ID_PREFIX = "o_";
}
