package TFG.Data_Analysis.Helpers;

import java.util.List;

public class FilterListDto {
    private List<String> titlesFilter;
    private List<Boolean> rowsWanted;

    public List<String> getTitlesFilter() {
        return titlesFilter;
    }

    public void setTitlesFilter(List<String> titlesFilter) {
        this.titlesFilter = titlesFilter;
    }

    public List<Boolean> getRowsWanted() {
        return rowsWanted;
    }

    public void setRowsWanted(List<Boolean> rowsWanted) {
        this.rowsWanted = rowsWanted;
    }
}
