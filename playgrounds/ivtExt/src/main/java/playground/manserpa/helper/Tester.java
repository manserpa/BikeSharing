package playground.manserpa.helper;

import java.util.ArrayList;
import java.util.ListIterator;


public class Tester {
	
	/**
	 * 
	 * This is a file used to test small mathematical operations in JAVA. Just to see how JAVA handles int, double and so on
	 * 
	 */
	
	public static void main(final String[] args)	{
		
		ArrayList<String> stopsToBeServed = new ArrayList<>();
		stopsToBeServed.add("para_9_38-163_56-Forth");
		stopsToBeServed.add("ajdsf_asdfö_A");
		stopsToBeServed.add("ajdsf_asdfö_A");
		stopsToBeServed.add("ajdsf_asdfö_B");
		stopsToBeServed.add("ajdsf_asdfö_B");
		stopsToBeServed.add("ajdsf_asdfö_A");
		stopsToBeServed.add("ajdsf_A");
		stopsToBeServed.add("ajdsf_asdfö_ljsaf_A");
		stopsToBeServed.add("ajdsf_asdfasdfö_A");
		
		
		ListIterator<String> listIterator = stopsToBeServed.listIterator(stopsToBeServed.size());
		while(listIterator.hasPrevious()) {
			String[] stopName = listIterator.previous().split("_");
			
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < stopName.length; i++)	{
				if(i != stopName.length - 1) {
					builder.append(stopName[i] + "_");
				} else {
					if(stopName[i].equals("A"))
						builder.append("B");
					if(stopName[i].equals("B"))
						builder.append("A");
				}
			}
			String str = builder.toString();
			
			
		}
		
		String[] stopIdSplit = "para_8224_A".split("_");
		String reversedStop = "";
		for(int i = 0; i < stopIdSplit.length - 1; i++)	{
			reversedStop += stopIdSplit[i] + "_";
		}
		if(stopIdSplit[stopIdSplit.length - 1].equals("A"))
			reversedStop += "B";
		else
			reversedStop += "A";
		System.out.println(reversedStop);
		
	}

}
