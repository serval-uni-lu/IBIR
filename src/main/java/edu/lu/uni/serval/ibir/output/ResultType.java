package edu.lu.uni.serval.ibir.output;

public enum ResultType {
    DEFAULT("DEF"), PCL_AND_DEFAULT("PCL_AND_DEFAULT"), PCL("PCL_IBIR_ONLY");

    private final String id;

    ResultType(String id) {
        this.id = id;
    }

    public static ResultType forId(String id) {
        if (id == null || id.isEmpty()) return DEFAULT;
        for (ResultType value : ResultType.values()){
            if (value.id.equalsIgnoreCase(id)) return value;
        }
        return DEFAULT;
    }
}
