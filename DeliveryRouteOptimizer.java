import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DeliveryRouteOptimizer {
    
    private static final String URL = "jdbc:mysql://localhost:3306/delivery_routes";
    private static final String USER = "root";
    private static final String PASSWORD = "password";  // Update with your DB password
    
    private Connection connection;

    public DeliveryRouteOptimizer() throws SQLException {
        // Establish connection to the database
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Function to get all locations from the database
    public Map<Integer, String> getLocations() throws SQLException {
        Map<Integer, String> locations = new HashMap<>();
        String query = "SELECT id, name FROM locations";
        
        try (Statement stmt = connection.createStatement(); 
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                locations.put(rs.getInt("id"), rs.getString("name"));
            }
        }
        
        return locations;
    }

    // Function to get all paths from the database (with dynamic traffic data)
    public Map<Integer, List<Edge>> getPaths(boolean realTimeTraffic) throws SQLException {
        Map<Integer, List<Edge>> graph = new HashMap<>();
        String query = "SELECT from_location, to_location, distance, traffic_factor FROM paths";
        
        // Simulate real-time traffic updates (adjust distances based on traffic factors)
        try (Statement stmt = connection.createStatement(); 
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int from = rs.getInt("from_location");
                int to = rs.getInt("to_location");
                double distance = rs.getDouble("distance");
                double trafficFactor = rs.getDouble("traffic_factor");  // factor for traffic delays
                
                // If real-time traffic data is enabled, adjust the distance accordingly
                if (realTimeTraffic) {
                    distance *= trafficFactor;  // Increase distance if traffic is bad
                }
                
                graph.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, distance));
                graph.computeIfAbsent(to, k -> new ArrayList<>()).add(new Edge(from, distance)); // assuming undirected graph
            }
        }
        
        return graph;
    }

    // Dijkstra's algorithm to find the shortest path from a start location to all other locations
    public Map<Integer, Double> dijkstra(Map<Integer, List<Edge>> graph, int start, String criterion) {
        Map<Integer, Double> distances = new HashMap<>();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(Comparator.comparingDouble(v -> v.distance));
        
        for (Integer node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        pq.add(new Vertex(start, 0.0));
        
        while (!pq.isEmpty()) {
            Vertex current = pq.poll();
            
            // If the shortest distance has been found, no need to process it further
            if (current.distance > distances.get(current.id)) {
                continue;
            }
            
            // Update distances for adjacent vertices
            for (Edge neighbor : graph.get(current.id)) {
                double newDist = current.distance + neighbor.distance;
                
                // Implement different optimization criteria based on the parameter (e.g., least time, least cost, etc.)
                if ("cost".equals(criterion)) {
                    newDist += 1.5 * neighbor.distance; // example of cost optimization (e.g., fuel cost)
                } else if ("time".equals(criterion)) {
                    newDist += 0.5 * neighbor.distance; // example of time optimization (e.g., based on road type)
                }
                
                if (newDist < distances.get(neighbor.to)) {
                    distances.put(neighbor.to, newDist);
                    pq.add(new Vertex(neighbor.to, newDist));
                }
            }
        }
        
        return distances;
    }

    // Function to simulate delivery window optimization (optimize delivery times)
    public Map<Integer, Double> optimizeForDeliveryWindows(Map<Integer, List<Edge>> graph, int start, Map<Integer, String> deliveryWindows) {
        Map<Integer, Double> distances = new HashMap<>();
        PriorityQueue<Vertex> pq = new PriorityQueue<>(Comparator.comparingDouble(v -> v.distance));
        
        for (Integer node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        pq.add(new Vertex(start, 0.0));
        
        while (!pq.isEmpty()) {
            Vertex current = pq.poll();
            
            if (current.distance > distances.get(current.id)) {
                continue;
            }
            
            for (Edge neighbor : graph.get(current.id)) {
                double newDist = current.distance + neighbor.distance;
                // Modify the path selection based on delivery time windows
                if (deliveryWindows.containsKey(neighbor.to)) {
                    String window = deliveryWindows.get(neighbor.to);
                    if (newDist > parseTimeFromWindow(window)) {
                        continue; // Skip paths outside of delivery window
                    }
                }
                if (newDist < distances.get(neighbor.to)) {
                    distances.put(neighbor.to, newDist);
                    pq.add(new Vertex(neighbor.to, newDist));
                }
            }
        }
        
        return distances;
    }

    // Helper method to simulate parsing of time windows (e.g., "09:00-12:00")
    private double parseTimeFromWindow(String timeWindow) {
        // Parse the time window and return the time as a double (e.g., 9.0 for 9:00 AM)
        String[] times = timeWindow.split("-");
        return Double.parseDouble(times[0].substring(0, 2));  // Simplified for example
    }

    public static void main(String[] args) {
        try {
            DeliveryRouteOptimizer optimizer = new DeliveryRouteOptimizer();
            
            // Load locations and paths from the database (with real-time traffic data)
            Map<Integer, String> locations = optimizer.getLocations();
            Map<Integer, List<Edge>> graph = optimizer.getPaths(true); // Enable real-time traffic data
            
            // Display all locations
            System.out.println("Locations:");
            locations.forEach((id, name) -> System.out.println(id + ": " + name));
            
            // Assume the start point is location with id 1 (just an example)
            int startLocation = 1;
            
            // Get the shortest paths based on different optimization criteria (e.g., least cost, least time)
            Map<Integer, Double> shortestPaths = optimizer.dijkstra(graph, startLocation, "cost");
            System.out.println("\nShortest paths from location " + locations.get(startLocation) + " based on cost optimization:");
            shortestPaths.forEach((id, distance) -> 
                System.out.println("To " + locations.get(id) + ": " + distance + " units"));
            
            // Assume delivery windows (e.g., "09:00-12:00") are available for locations
            Map<Integer, String> deliveryWindows = new HashMap<>();
            deliveryWindows.put(2, "09:00-12:00");
            deliveryWindows.put(3, "14:00-17:00");
            
            // Optimize routes based on delivery windows
            Map<Integer, Double> deliveryOptimizedPaths = optimizer.optimizeForDeliveryWindows(graph, startLocation, deliveryWindows);
            System.out.println("\nDelivery optimized paths from location " + locations.get(startLocation) + ":");
            deliveryOptimizedPaths.forEach((id, distance) -> 
                System.out.println("To " + locations.get(id) + ": " + distance + " units"));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Helper class to represent edges in the graph
    static class Edge {
        int to;
        double distance;
        
        public Edge(int to, double distance) {
            this.to = to;
            this.distance = distance;
        }
    }

    // Helper class to represent vertices in the priority queue
    static class Vertex {
        int id;
        double distance;
        
        public Vertex(int id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
