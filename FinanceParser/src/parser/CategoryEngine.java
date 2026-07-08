package parser;
import db.DBConnection;
import java.sql.*;
import java.util.*;
// CategoryEngine — auto-assigns category_id from merchant name using DB rules
public class CategoryEngine {
    private static class Rule { String keyword,matchType; int categoryId,priority; }
    private static List<Rule> cache=null;

    private static void loadRules() {
        if (cache!=null) return; cache=new ArrayList<>();
        try {
            ResultSet rs=DBConnection.getConnection().createStatement().executeQuery(
                    "SELECT keyword,category_id,match_type,priority FROM CATEGORY_RULES WHERE is_active=TRUE ORDER BY priority");
            while (rs.next()) {
                Rule r=new Rule(); r.keyword=rs.getString("keyword").toLowerCase();
                r.categoryId=rs.getInt("category_id"); r.matchType=rs.getString("match_type");
                r.priority=rs.getInt("priority"); cache.add(r);
            }
            System.out.println("[CategoryEngine] Loaded "+cache.size()+" rules.");
        } catch (SQLException e) { System.err.println("[CategoryEngine] ERROR: "+e.getMessage()); }
    }

    public static int getCategoryId(String merchantName) {
        loadRules(); if (merchantName==null||merchantName.isEmpty()) return 1;
        String lower=merchantName.toLowerCase().trim();
        for (Rule r:cache) {
            boolean matched=switch(r.matchType) {
                case "CONTAINS"->lower.contains(r.keyword);
                case "EXACT"->lower.equals(r.keyword);
                case "STARTS"->lower.startsWith(r.keyword);
                default->false;
            };
            if (matched) { System.out.printf("  [CE] \"%s\" → cat=%d (rule:%s)%n",merchantName,r.categoryId,r.keyword); return r.categoryId; }        }
        return 1;
    }
    public static void reloadRules() { cache=null; loadRules(); }
}

