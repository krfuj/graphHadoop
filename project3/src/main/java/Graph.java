import java.io.*;
import java.util.Scanner;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;



class Tagged implements Writable {
    public boolean tag;                // true for a graph vertex, false for distance
    public int distance;               // the distance from the starting vertex
    public Vector<Integer> following;  // the vertex neighbors

    Tagged () { tag = false; }
    Tagged ( int d ) { tag = false; distance = d; }
    Tagged ( int d, Vector<Integer> f ) { tag = true; distance = d; following = f; }

    public void write ( DataOutput out ) throws IOException {
        out.writeBoolean(tag);
        out.writeInt(distance);
        if (tag) {
            out.writeInt(following.size());
            for ( int i = 0; i < following.size(); i++ )
                out.writeInt(following.get(i));
        }
    }

    public void readFields ( DataInput in ) throws IOException {
        tag = in.readBoolean();
        distance = in.readInt();
        if (tag) {
            int n = in.readInt();
            following = new Vector<Integer>(n);
            for ( int i = 0; i < n; i++ )
                following.add(in.readInt());
        }
    }
}


public class Graph {
    static int start_id = 14701391;
    static int max_int = Integer.MAX_VALUE;


    static class Map1 extends Mapper<LongWritable,Text,IntWritable,IntWritable> {
        public void map ( LongWritable key, Text value, Context context )
                throws IOException, InterruptedException {
            Scanner s = new Scanner(value.toString()).useDelimiter(",");
            int id = s.nextInt();
            int follower = s.nextInt();
            context.write(new IntWritable(follower), new IntWritable(id));
            s.close();
        }
    }

    static class Reduce1 extends Reducer<IntWritable,IntWritable,IntWritable,Tagged> {
        public void reduce ( IntWritable key, Iterable<IntWritable> values, Context context )
                throws IOException, InterruptedException {
            Vector<Integer> following = new Vector<Integer>();
            for (IntWritable val : values)
                following.add(val.get());
            context.write(key, new Tagged(max_int, following));
        }
    }



        static class Map2 extends Mapper<IntWritable,Tagged,IntWritable,Tagged> {
            public void map ( IntWritable key, Tagged value, Context context )
                    throws IOException, InterruptedException {
                context.write(key,value);
                if (value.tag) {
                    for (int id : value.following)
                        context.write(new IntWritable(id), new Tagged(value.distance+1));
                }
            }
        }


        static class Reduce2 extends Reducer<IntWritable, Tagged, IntWritable, Tagged> {
            public void reduce ( IntWritable key, Iterable<Tagged> values, Context context )
                    throws IOException, InterruptedException {
                // Keep track of the minimum distance between the source vertex and any of its followers.
                int minDistance = Integer.MAX_VALUE;

                // Keep track of the list of followers of the source vertex.
                Vector<Integer> following = new Vector<Integer>();

                // Iterate over the values and update the minimum distance and the list of followers accordingly.
                for (Tagged val : values) {
                    if (val.distance < minDistance) {
                        minDistance = val.distance;
                        following = (Vector<Integer>)val.following.clone();
                    }
                }

                // Emit the output.
                context.write(key, new Tagged(minDistance, following));
            }
        }



    static class Map3 extends Mapper<IntWritable, Tagged, IntWritable, IntWritable> {
        public void map ( IntWritable key, Tagged value, Context context )
                throws IOException, InterruptedException {
            // If the distance between the source vertex and the destination vertex is less than Integer.MAX_VALUE, emit a (key, value) pair, where the key is the vertex and the value is the distance between the source vertex and the destination vertex.
            if (value.distance < Integer.MAX_VALUE) {
                context.write(key, new IntWritable(value.distance));
            }
        }
    }


    public static void main ( String[] args ) throws Exception {

        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(conf);
        int iterations = 5;

        // First Map-Reduce job
        Job job = Job.getInstance(conf);
        job.setJobName("First Map-Reduce Job");
        job.setJarByClass(Graph.class);
        job.setMapperClass(Map1.class);
        job.setReducerClass(Reduce1.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Tagged.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.setInputPaths(job,new Path(args[0]));
        FileOutputFormat.setOutputPath(job,new Path(args[1]+"0"));
        job.waitForCompletion(true);

        for ( short i = 0; i < iterations; i++ ) {
            // Second Map-Reduce job
            job = Job.getInstance(conf);
            job.setJobName("Second Map-Reduce Job");
            job.setJarByClass(Graph.class);
            job.setMapperClass(Map2.class);
            job.setReducerClass(Reduce2.class);
            job.setMapOutputKeyClass(IntWritable.class);
            job.setMapOutputValueClass(Tagged.class);
            job.setOutputKeyClass(IntWritable.class);
            job.setOutputValueClass(Tagged.class);
            job.setInputFormatClass(SequenceFileInputFormat.class);
            job.setOutputFormatClass(SequenceFileOutputFormat.class);

            FileInputFormat.setInputPaths(job,new Path(args[1]+i));
            FileOutputFormat.setOutputPath(job,new Path(args[1]+(i+1)));
            job.waitForCompletion(true);
        }

        // Third Map-Reduce job
        job = Job.getInstance(conf);
        job.setJobName("Third Map-Reduce Job");
        job.setJarByClass(Graph.class);
        job.setMapperClass(Map3.class);
        // No reducer for this job
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job,new Path(args[1]+iterations));
        FileOutputFormat.setOutputPath(job,new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
