package TFG.Data_Analysis.Helpers;

public class Pair <C, V>{
    private C column;
    private V value;

    public Pair() {
    }

    public Pair(C column, V value) {
        this.column = column;
        this.value = value;
    }

    public C getColumn() {
        return column;
    }

    public void setColumn(C column) {
        this.column = column;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
