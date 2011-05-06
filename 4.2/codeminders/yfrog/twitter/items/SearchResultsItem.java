package codeminders.yfrog.twitter.items;

import java.util.Hashtable;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class SearchResultsItem extends JsonItemBase implements Persistable {

    public long MaxID = 0;
    public long SinceID = 0;
    public String RefreshUrl = null;
    public String NextPage = null;
    public int ResultsPerPage = 0;
    public int Page = 0;
    public double CompletedIn = 0.0;
    public String Query = null;

    public SearchResultItem[] Results = null;

    public SearchResultsItem() { }

    protected void setField(String name, Object value) {
        if ("max_id".equals(name)) this.MaxID = Long.parseLong(value.toString());
        else if ("since_id".equals(name)) this.SinceID = Long.parseLong(value.toString());
        else if ("refresh_url".equals(name)) this.RefreshUrl = value.toString();
        else if ("next_page".equals(name)) this.NextPage = value.toString();
        else if ("results_per_page".equals(name)) this.ResultsPerPage = Integer.parseInt(value.toString());
        else if ("page".equals(name)) this.Page = Integer.parseInt(value.toString());
        else if ("completed_in".equals(name)) this.CompletedIn = Double.parseDouble(value.toString());
        else if ("query".equals(name)) this.Query = ItemBase.parseText(value.toString());
        else if ("results".equals(name)) {
            Object[] values = (Object[])value;
            this.Results = new SearchResultItem[values.length];
            for (int i = values.length - 1; i >= 0; i--) {
                this.Results[i] = new SearchResultItem();
                this.Results[i].setFields((Hashtable)values[i]);
            }
        }
        else super.setField(name, value);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Results); }
}

