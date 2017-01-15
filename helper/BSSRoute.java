class BSSRoute {
	private String startid;
	private String endid;
	private double tt;
	private double dis;
	private double disImaginary;
	private double heightDiff;
	
	BSSRoute(String startid, String endid, double tt, double dis) {
		this.startid = startid;
		this.endid = endid;
		this.tt = tt;
		this.dis = dis;
	}

	BSSRoute(String startid, String endid, double tt, double dis, double disImaginary, double heightDiff) {
		this.startid = startid;
		this.endid = endid;
		this.tt = tt;
		this.dis = dis;
		this.disImaginary = disImaginary;
		this.heightDiff = heightDiff;
	}

	String getStartid() {
		return startid;
	}

	String getEndid() {
		return endid;
	}

	double getTt() {
		return tt;
	}

	double getDis() {
		return dis;
	}

	double getDisImaginary() {
		return disImaginary;
	}

	double getHeightDiff() {
		return heightDiff;
	}
	
	
	

}
