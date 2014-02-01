package it.thepund.timeliner.bur;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.sail.memory.MemoryStore;

/*
 * Writes to the OUTPUT file an RDF created by merging all the 
 * RDF representations of the letters hosted at burckhardtsource.org
 */

public class MurucaRDFHarvester {
	
	
	private static String OUTPUT= "";
	
	//All the ids of the letters in the DL. TODO: it should be provided by a Muruca API
	private static final int[] ids = new int[] {2,3,4,6,7,8,9,10,11,12,13,16,17,23,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,53,57,58,59,60,61,62,63,65,66,67,68,69,70,75,76,77,78,79,80,81,82,83,84,86,87,88,89,91,92,93,94,96,97,98,99,102,103,105,107,109,111,113,115,116,117,118,119,120,121,122,123,125,126,127,128,130,131,132,133,134,135,136,137,138,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,173,175,176,177,178,179,180,182,188,189,190,191,195,196,201,204,205,206,210,211,212,213,214,221,225,226,229,232,237,238,239,241,242,243,244,245,247,248,254,255,257,258,259,261,262,270,271,272,273,275,276,277,278,279,280,281,282,283,284,285,288,289,299,300,306,308,313,314,324,325,326,328,333,344,348,350,353,356,357,359,360,362,363,364,365,366,367,369,370,371,372,376,377,378,379,380,381,382,383,384,385,386,387,388,389,390,391,395,396,397,398,399,400,402,404,410,411,412,413,414,418,419,420,421,422,423,424,426,427,428,429,430,431,433,434,435,436,437,438,439,440,441,442,443,444,445,448,449,450,451,452,453,454,455,457,458,459,460,461,462,463,464,465,466,467,468,470,471,472,473,475,476,477,478,479,480,481,482,483,484,485,486,488,489,491,492,495,496,497,498,501,502,503,506,508,509,511,512,513,514,516,517,518,519,521,522,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,541,542,543,544,545,546,548,550,551,555,556,557,559,561,564,565,567,568,569,570,571,574,575,576,577,578,579,580,581,582,583,584,585,592,593,594,595,596,597,598,599,601,602,603,605,606,607,608,609,610,611,614,615,616,617,618,619,620,621,623,624,626,627,628,629,631,640,641,642,643,644,645,647,648,649,650,652,653,654,655,656,658,659,660,661,662,664,668,669,670,671,672,673,674,675,676,677,678,679,680,681,682,683,684,685,686,687,688,689,690,691,692,693,697,698,700,701,702,704,706,707,708,709,710,711,713,714,715,716,717,719,720,721,722,723,724,725,726,727,728,729,731,732,733,734,735,736,737,738,740,741,742,743,744,745,746,747,748,749,750,751,752,753,754,755,757,758,759,760,761,763,764,765,766,767,768,770,771,772,773,774,775,776,777,778,779,780,781,782,783,784,785,786,787,788,789,790,791,792,793,794,795,796,798,799,800,801,802,803,804,805,806,807,808,810,811,812,813,814,815,816,817,818,819,820,822,823,824,825,826,827,828,829,830,831,832,833,834,835,836,837,838,839,840,841,842,843,847,848,849,850,851,854,855,856,857,858,859,860,862,863,864,865,866,867,868,869,875,876};
	private static final String namespace = "http://burckhardtsource.org/letter/";
	private static final String suffix = ".rdf";
	
	public static void main(String[] args) throws RepositoryException, RDFHandlerException, RDFParseException, MalformedURLException, IOException {
		
		
		if (args.length == 1) {
			OUTPUT = args[0];
		} else {
			System.out.println("Wrong arguments! Exiting....");
			return;
		}
		
		//Create and initialize the Sesame repository
		Repository rep = new SailRepository(new MemoryStore());
				
		rep.initialize();	
				
		RepositoryConnection conn = rep.getConnection();
				
		for (int id : ids) {
			String url = namespace + id + suffix;
			conn.add(new URL(url),"",RDFFormat.RDFXML);
		}
		
		
		RDFHandler rdfxmlWriter = new RDFXMLPrettyWriter(new FileOutputStream(OUTPUT));
		conn.export(rdfxmlWriter);
		
		conn.close();
	}

}
