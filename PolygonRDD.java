package GeoSpark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import Functions.PartitionAssignGridPolygon;
import Functions.PolygonRangeFilter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class PolygonRDD implements Serializable{
	private JavaRDD<Polygon> polygonRDD;
	public PolygonRDD(JavaRDD<Polygon> polygonRDD)
	{
		this.setPolygonRDD(polygonRDD);
	}
	public PolygonRDD(JavaSparkContext spark, String InputLocation)
	{
		this.setPolygonRDD(spark.textFile(InputLocation).map(new Function<String,Polygon>()
			{
			public Polygon call(String s)
			{	
				List<String> input=Arrays.asList(s.split(","));
				ArrayList<Coordinate> coordinatesList = new ArrayList<Coordinate>();
				/*for(int i=0;i<input.size();i=i+2)
				{
					coordinatesList.add(new Coordinate(Double.parseDouble(input.get(i)),Double.parseDouble(input.get(i+1))));
				}*/
				coordinatesList.add(new Coordinate(Double.parseDouble(input.get(0)),Double.parseDouble(input.get(1))));
				coordinatesList.add(new Coordinate(Double.parseDouble(input.get(0)),Double.parseDouble(input.get(3))));
				coordinatesList.add(new Coordinate(Double.parseDouble(input.get(2)),Double.parseDouble(input.get(3))));
				coordinatesList.add(new Coordinate(Double.parseDouble(input.get(2)),Double.parseDouble(input.get(1))));
				coordinatesList.add(new Coordinate(Double.parseDouble(input.get(0)),Double.parseDouble(input.get(1))));
				Coordinate[] coordinates=new Coordinate[coordinatesList.size()];
				coordinates=coordinatesList.toArray(coordinates);
				GeometryFactory fact = new GeometryFactory();
				 LinearRing linear = new GeometryFactory().createLinearRing(coordinates);
				 Polygon polygon = new Polygon(linear, null, fact);
				 return polygon;
			}
			}));
	}
	public JavaRDD<Polygon> getPolygonRDD() {
		return polygonRDD;
	}
	public void setPolygonRDD(JavaRDD<Polygon> polygonRDD) {
		this.polygonRDD = polygonRDD;
	}
	public void rePatitrtition(Integer number)
	{
		this.polygonRDD=this.polygonRDD.repartition(number);
	}
	public PolygonRDD SpatialRangeQuery(Envelope envelope,Integer condition)
	{
		JavaRDD<Polygon> result=this.polygonRDD.filter(new PolygonRangeFilter(envelope,condition));
		return new PolygonRDD(result);
	}
	public PolygonRDD SpatialRangeQuery(Polygon polygon,Integer condition)
	{
		JavaRDD<Polygon> result=this.polygonRDD.filter(new PolygonRangeFilter(polygon,condition));
		return new PolygonRDD(result);
	}
	public RectangleRDD MinimumBoundingRectangle()
	{
	JavaRDD<Envelope> rectangleRDD=this.polygonRDD.map(new Function<Polygon,Envelope>(){
			
			public Envelope call(Polygon s)
			{
				Envelope MBR= s.getEnvelopeInternal();
				return MBR;
			}
		});
		return new RectangleRDD(rectangleRDD);
	}
	public JavaPairRDD<Polygon,String> SpatialJoinQuery(PolygonRDD polygonRDD,Integer Condition,Integer GridNumberHorizontal,Integer GridNumberVertical)
	{
		//Find the border of both of the two datasets---------------
		final Integer condition=Condition;
		//condition=0 means only consider fully contain in query, condition=1 means consider full contain and partial contain(overlap).
	
		Double minLongtitudeQueryAreaSet;
		Double maxLongtitudeQueryAreaSet;
		Double minLatitudeQueryAreaSet;
		Double maxLatitudeQueryAreaSet;
		Double minLongtitudeTargetSet;
		Double maxLongtitudeTargetSet;
		Double minLatitudeTargetSet;
		Double maxLatitudeTargetSet;
		Double minLongtitude1QueryAreaSet=polygonRDD.getPolygonRDD().min(new PolygonXMinComparator()).getEnvelopeInternal().getMinX();
		Double maxLongtitude1QueryAreaSet=polygonRDD.getPolygonRDD().max(new PolygonXMinComparator()).getEnvelopeInternal().getMinX();
		Double minLatitude1QueryAreaSet=polygonRDD.getPolygonRDD().min(new PolygonYMinComparator()).getEnvelopeInternal().getMinY();
		Double maxLatitude1QueryAreaSet=polygonRDD.getPolygonRDD().max(new PolygonYMinComparator()).getEnvelopeInternal().getMinY();
		Double minLongtitude2QueryAreaSet=polygonRDD.getPolygonRDD().min(new PolygonXMaxComparator()).getEnvelopeInternal().getMaxX();
		Double maxLongtitude2QueryAreaSet=polygonRDD.getPolygonRDD().max(new PolygonXMaxComparator()).getEnvelopeInternal().getMaxX();
		Double minLatitude2QueryAreaSet=polygonRDD.getPolygonRDD().min(new PolygonYMaxComparator()).getEnvelopeInternal().getMaxY();
		Double maxLatitude2QueryAreaSet=polygonRDD.getPolygonRDD().max(new PolygonYMaxComparator()).getEnvelopeInternal().getMaxY();
		Double minLongtitude1TargetSet=this.polygonRDD.min(new PolygonXMinComparator()).getEnvelopeInternal().getMinX();
		Double maxLongtitude1TargetSet=this.polygonRDD.max(new PolygonXMinComparator()).getEnvelopeInternal().getMinX();
		Double minLatitude1TargetSet=this.polygonRDD.min(new PolygonYMinComparator()).getEnvelopeInternal().getMinY();
		Double maxLatitude1TargetSet=this.polygonRDD.max(new PolygonYMinComparator()).getEnvelopeInternal().getMinY();
		Double minLongtitude2TargetSet=this.polygonRDD.min(new PolygonXMaxComparator()).getEnvelopeInternal().getMaxX();
		Double maxLongtitude2TargetSet=this.polygonRDD.max(new PolygonXMaxComparator()).getEnvelopeInternal().getMaxX();
		Double minLatitude2TargetSet=this.polygonRDD.min(new PolygonYMaxComparator()).getEnvelopeInternal().getMaxY();
		Double maxLatitude2TargetSet=this.polygonRDD.max(new PolygonYMaxComparator()).getEnvelopeInternal().getMaxY();
		//QueryAreaSet min/max longitude and latitude
		if(minLongtitude1QueryAreaSet<minLongtitude2QueryAreaSet)
		{
			minLongtitudeQueryAreaSet=minLongtitude1QueryAreaSet;
		}
		else
		{
			minLongtitudeQueryAreaSet=minLongtitude2QueryAreaSet;
		}
		if(maxLongtitude1QueryAreaSet>maxLongtitude2QueryAreaSet)
		{
			maxLongtitudeQueryAreaSet=maxLongtitude1QueryAreaSet;
		}
		else
		{
			maxLongtitudeQueryAreaSet=maxLongtitude2QueryAreaSet;
		}
		if(minLatitude1QueryAreaSet<minLatitude2QueryAreaSet)
		{
			minLatitudeQueryAreaSet=minLatitude1QueryAreaSet;
		}
		else
		{
			minLatitudeQueryAreaSet=minLatitude2QueryAreaSet;
		}
		if(maxLatitude1QueryAreaSet>maxLatitude2QueryAreaSet)
		{
			maxLatitudeQueryAreaSet=maxLatitude1QueryAreaSet;
		}
		else
		{
			maxLatitudeQueryAreaSet=maxLatitude2QueryAreaSet;
		}
		//TargetSet min/max longitude and latitude
		if(minLongtitude1TargetSet<minLongtitude2TargetSet)
		{
			minLongtitudeTargetSet=minLongtitude1TargetSet;
		}
		else
		{
			minLongtitudeTargetSet=minLongtitude2TargetSet;
		}
		if(maxLongtitude1TargetSet>maxLongtitude2TargetSet)
		{
			maxLongtitudeTargetSet=maxLongtitude1TargetSet;
		}
		else
		{
			maxLongtitudeTargetSet=maxLongtitude2TargetSet;
		}
		if(minLatitude1TargetSet<minLatitude2TargetSet)
		{
			minLatitudeTargetSet=minLatitude1TargetSet;
		}
		else
		{
			minLatitudeTargetSet=minLatitude2TargetSet;
		}
		if(maxLatitude1TargetSet>maxLatitude2TargetSet)
		{
			maxLatitudeTargetSet=maxLatitude1TargetSet;
		}
		else
		{
			maxLatitudeTargetSet=maxLatitude2TargetSet;
		}
		//Border found
		Double minLongitude=minLongtitudeTargetSet;
		Double minLatitude=minLatitudeTargetSet;
		Double maxLongitude=maxLongtitudeTargetSet;
		Double maxLatitude=maxLatitudeTargetSet;
		if(minLongitude>minLongtitudeQueryAreaSet)
		{
			minLongitude=minLongtitudeQueryAreaSet;
		}
		if(maxLongitude<maxLongtitudeQueryAreaSet)
		{
			maxLongitude=maxLongtitudeQueryAreaSet;
		}
		if(minLatitude>minLatitudeQueryAreaSet)
		{
			minLatitude=minLatitudeQueryAreaSet;
		}
		if(maxLatitude<maxLatitudeQueryAreaSet)
		{
			maxLatitude=maxLatitudeQueryAreaSet;
		}
//Build Grid file-------------------
		Double[] gridHorizontalBorder = new Double[GridNumberHorizontal+1];
		Double[] gridVerticalBorder=new Double[GridNumberVertical+1];
		double LongitudeIncrement=(maxLongitude-minLongitude)/GridNumberHorizontal;
		double LatitudeIncrement=(maxLatitude-minLatitude)/GridNumberVertical;
		System.out.println(maxLongitude);
		System.out.println(minLongitude);
		System.out.println(maxLatitude);
		System.out.println(minLatitude);
		for(int i=0;i<GridNumberHorizontal+1;i++)
		{
			gridHorizontalBorder[i]=minLongitude+LongitudeIncrement*i;
		}
		for(int i=0;i<GridNumberVertical+1;i++)
		{
			gridVerticalBorder[i]=minLatitude+LatitudeIncrement*i;
		}
		//Assign grid ID to both of the two dataset---------------------
		JavaPairRDD<Integer,Polygon> TargetSetWithID=this.polygonRDD.mapPartitionsToPair(new PartitionAssignGridPolygon(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
		JavaPairRDD<Integer,Polygon> QueryAreaSetWithID=polygonRDD.getPolygonRDD().mapPartitionsToPair(new PartitionAssignGridPolygon(GridNumberHorizontal,GridNumberVertical,gridHorizontalBorder,gridVerticalBorder));
//Join two dataset
		JavaPairRDD<Integer, Tuple2<Iterable<Polygon>, Iterable<Polygon>>> jointSet=QueryAreaSetWithID.cogroup(TargetSetWithID).repartition((QueryAreaSetWithID.partitions().size()+TargetSetWithID.partitions().size())*2);
		//Calculate the relation between one point and one query area
				JavaPairRDD<Polygon,Polygon> queryResult=jointSet.flatMapToPair(new PairFlatMapFunction<Tuple2<Integer,Tuple2<Iterable<Polygon>, Iterable<Polygon>>>, Polygon,Polygon>()
						{

					public Iterable<Tuple2<Polygon, Polygon>> call(
							Tuple2<Integer, Tuple2<Iterable<Polygon>, Iterable<Polygon>>> t)
							throws Exception {
						ArrayList<Tuple2<Polygon, Polygon>> QueryAreaAndTarget=new ArrayList();
						Iterator<Polygon> QueryAreaIterator=t._2()._1().iterator();
						
						while(QueryAreaIterator.hasNext())
						{
							Polygon currentQueryArea=QueryAreaIterator.next();
							Iterator<Polygon> TargetIterator=t._2()._2().iterator();
							while(TargetIterator.hasNext())
							{
								Polygon currentTarget=TargetIterator.next();
								if(condition==0){
								if(currentQueryArea.contains(currentTarget))
								{
									QueryAreaAndTarget.add(new Tuple2<Polygon,Polygon>(currentQueryArea,currentTarget));
								}
								}
								else
								{
									if(currentQueryArea.intersects(currentTarget))
									{
										QueryAreaAndTarget.add(new Tuple2<Polygon,Polygon>(currentQueryArea,currentTarget));
									}
								}
							}
						}
						
						return QueryAreaAndTarget;
					}
			
				});
		//Delete the duplicate result
				JavaPairRDD<Polygon, Iterable<Polygon>> aggregatedResult=queryResult.groupByKey();
				JavaPairRDD<Polygon,String> refinedResult=aggregatedResult.mapToPair(new PairFunction<Tuple2<Polygon,Iterable<Polygon>>,Polygon,String>()
						{

							public Tuple2<Polygon, String> call(Tuple2<Polygon, Iterable<Polygon>> t)
									{
								Integer commaFlag=0;
								Iterator<Polygon> valueIterator=t._2().iterator();
								String result="";
								while(valueIterator.hasNext())
								{
									Polygon currentTarget=valueIterator.next();
									Coordinate[] polygonCoordinate=currentTarget.getCoordinates();
									Integer count=polygonCoordinate.length;
									String currentTargetString="";
									for(int i=0;i<count;i++)
									{
										if(currentTargetString==""){
										currentTargetString=currentTargetString+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
									
										}
										else currentTargetString=currentTargetString+","+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
									}
									if(!result.contains(currentTargetString))
									{
										if(commaFlag==0)
										{
											result=result+currentTargetString;
											commaFlag=1;
										}
										else result=result+","+currentTargetString;
									}
								}
								
								return new Tuple2<Polygon, String>(t._1(),result);
							}
					
						});
				//Persist the result on HDFS
				return refinedResult;

	}
JavaPairRDD<Polygon,String> SpatialJoinQueryWithMBR(PolygonRDD polygonRDD,Integer Condition,Integer GridNumberHorizontal,Integer GridNumberVertical)
	{
	final Integer condition=Condition;
//Create mapping between MBR and polygon
	JavaPairRDD<Envelope,Polygon> polygonRDDwithKey1=polygonRDD.getPolygonRDD().mapToPair(new PairFunction<Polygon,Envelope,Polygon>(){
		
		public Tuple2<Envelope,Polygon> call(Polygon s)
		{
			Envelope MBR= s.getEnvelopeInternal();
			return new Tuple2<Envelope,Polygon>(MBR,s);
		}
	}).repartition(polygonRDD.getPolygonRDD().partitions().size()*2);	
	JavaPairRDD<Envelope,Polygon> polygonRDDwithKey2=this.polygonRDD.mapToPair(new PairFunction<Polygon,Envelope,Polygon>(){
			
			public Tuple2<Envelope,Polygon> call(Polygon s)
			{
				Envelope MBR= s.getEnvelopeInternal();
				return new Tuple2<Envelope,Polygon>(MBR,s);
			}
		}).repartition(this.polygonRDD.partitions().size()*2);
	
//Filter phase
		RectangleRDD rectangleRDD1=this.MinimumBoundingRectangle();
		RectangleRDD rectangleRDD2=polygonRDD.MinimumBoundingRectangle();
		JavaPairRDD<Envelope,String> filterResult=rectangleRDD1.SpatialJoinQuery(rectangleRDD2, Condition, GridNumberHorizontal, GridNumberVertical);
//Refine phase
		//Exchange the key and the value
		JavaPairRDD<Envelope,Iterable<Envelope>> filterResultIterable=filterResult.mapToPair(new PairFunction<Tuple2<Envelope,String>,Envelope,Iterable<Envelope>>()
				{

					public Tuple2<Envelope, Iterable<Envelope>> call(
							Tuple2<Envelope, String> t)  {
						
						String currentTargetString=t._2();
						ArrayList<Envelope> Target=new ArrayList<Envelope>();
						
						Iterator<String> stringIterator=Arrays.asList(currentTargetString.split(",")).iterator();
						
						while(stringIterator.hasNext())
						{
							Envelope envelope=new Envelope(Double.parseDouble(stringIterator.next()),Double.parseDouble(stringIterator.next()),Double.parseDouble(stringIterator.next()),Double.parseDouble(stringIterator.next()));
							Target.add(envelope);
						}
						
						return new Tuple2<Envelope,Iterable<Envelope>>(t._1(),Target);
					}
			
				});
		JavaPairRDD<Envelope,Envelope> filterResultTransformed=filterResultIterable.flatMapToPair(new PairFlatMapFunction<Tuple2<Envelope,Iterable<Envelope>>,Envelope,Envelope>()
				{

					public Iterable<Tuple2<Envelope, Envelope>> call(
							Tuple2<Envelope, Iterable<Envelope>> t){
						ArrayList<Tuple2<Envelope,Envelope>> result=new ArrayList<Tuple2<Envelope,Envelope>>();
						Iterator<Envelope> targetIterator=t._2().iterator();
						while(targetIterator.hasNext())
						{
							Envelope currentTarget=targetIterator.next();
							result.add(new Tuple2<Envelope,Envelope>(t._1(),currentTarget));
						}
						return result;
					}
			
				});
		//Find the mapping polygon for every query MBR and then exchange the location with the query value
		JavaPairRDD<Envelope, Tuple2<Iterable<Polygon>, Iterable<Envelope>>> findPolygon1=polygonRDDwithKey1.cogroup(filterResultTransformed).repartition((polygonRDDwithKey1.partitions().size()+filterResultTransformed.partitions().size())*2);
		JavaPairRDD<Envelope,Polygon> findPolygon2=findPolygon1.flatMapToPair(new PairFlatMapFunction<Tuple2<Envelope,Tuple2<Iterable<Polygon>,Iterable<Envelope>>>,Envelope,Polygon>()
				{

					public Iterable<Tuple2<Envelope,Polygon>> call(
							Tuple2<Envelope, Tuple2<Iterable<Polygon>, Iterable<Envelope>>> t){
						ArrayList<Tuple2<Envelope,Polygon>> QueryAreaAndTarget=new ArrayList<Tuple2<Envelope,Polygon>>();
						Iterator<Polygon> QueryAreaIterator=t._2()._1().iterator();
						
						while(QueryAreaIterator.hasNext())
						{
							Polygon currentQueryArea=QueryAreaIterator.next();
							Iterator<Envelope> TargetIterator=t._2()._2().iterator();
							while(TargetIterator.hasNext())
							{
								Envelope currentTarget=TargetIterator.next();
								QueryAreaAndTarget.add(new Tuple2<Envelope,Polygon>(currentTarget,currentQueryArea));
							}
						
						}
						return QueryAreaAndTarget;
					}
			
				});
		JavaPairRDD<Envelope, Tuple2<Iterable<Polygon>, Iterable<Polygon>>> findPolygon3=polygonRDDwithKey2.cogroup(findPolygon2).repartition((polygonRDDwithKey2.partitions().size()+findPolygon2.partitions().size()));
		JavaPairRDD<Polygon,Polygon> filterResultWithPolygon=findPolygon3.flatMapToPair(new PairFlatMapFunction<Tuple2<Envelope,Tuple2<Iterable<Polygon>,Iterable<Polygon>>>,Polygon,Polygon>()
				{

					public Iterable<Tuple2<Polygon, Polygon>> call(Tuple2<Envelope, Tuple2<Iterable<Polygon>, Iterable<Polygon>>> t){
						
						
						ArrayList<Tuple2<Polygon,Polygon>> QueryAreaAndTarget=new ArrayList<Tuple2<Polygon,Polygon>>();
						Iterator<Polygon> QueryAreaIterator=t._2()._1().iterator();
						
						while(QueryAreaIterator.hasNext())
						{
							Polygon currentQueryArea=QueryAreaIterator.next();
							Iterator<Polygon> TargetIterator=t._2()._2().iterator();
							while(TargetIterator.hasNext())
							{
								Polygon currentTarget=TargetIterator.next();
								QueryAreaAndTarget.add(new Tuple2<Polygon,Polygon>(currentTarget,currentQueryArea));
							}
						
						}
						return QueryAreaAndTarget;
					}
			
				});
		//Query on polygons instead of MBR
		JavaPairRDD<Polygon,Iterable<Polygon>> groupedFilterResult=filterResultWithPolygon.groupByKey();
		JavaPairRDD<Polygon,String> refinedResult=groupedFilterResult.mapToPair(new PairFunction<Tuple2<Polygon,Iterable<Polygon>>,Polygon,String>()
				{

					public Tuple2<Polygon, String> call(Tuple2<Polygon, Iterable<Polygon>>t) {
						Integer commaFlag=0;
						Iterator<Polygon> valueIterator=t._2().iterator();
						String result="";
						while(valueIterator.hasNext())
						{
							Polygon currentTarget=valueIterator.next();
							if(condition==0){
							if(t._1().contains(currentTarget)){
							Coordinate[] polygonCoordinate=currentTarget.getCoordinates();
							Integer count=polygonCoordinate.length;
							String currentTargetString="";
							for(int i=0;i<count;i++)
							{
								if(currentTargetString==""){
								currentTargetString=currentTargetString+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
							
								}
								else currentTargetString=currentTargetString+","+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
							}
							if(!result.contains(currentTargetString))
							{
								if(commaFlag==0)
								{
									result=result+currentTargetString;
									commaFlag=1;
								}
								else result=result+","+currentTargetString;
							}
							}
							}
							else
							{
								if(t._1().intersects(currentTarget)){
									Coordinate[] polygonCoordinate=currentTarget.getCoordinates();
									Integer count=polygonCoordinate.length;
									String currentTargetString="";
									for(int i=0;i<count;i++)
									{
										if(currentTargetString==""){
										currentTargetString=currentTargetString+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
									
										}
										else currentTargetString=currentTargetString+","+polygonCoordinate[i].x+","+polygonCoordinate[i].y;
									}
									if(!result.contains(currentTargetString))
									{
										if(commaFlag==0)
										{
											result=result+currentTargetString;
											commaFlag=1;
										}
										else result=result+","+currentTargetString;
									}
									}
							}
						}
						
						return new Tuple2<Polygon, String>(t._1(),result);
					}
			
				});
//Return the refined result
		return refinedResult;
	}
}
