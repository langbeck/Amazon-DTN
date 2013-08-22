package br.ufpa.dtn;

import java.util.regex.Pattern;

public class Regex {

	public static void main(String[] args) {
		final Pattern regex = Pattern.compile("((e+u+)? *t+[ei]*)? *a+m+o+r* *(v+o*c+e*|t+u+)?", Pattern.CASE_INSENSITIVE);
		final String vet[] = {"Eu te AMO MILLY PINTO", " TE AMoooO", " Te amo", " Amo você", " Te amo", " Te amo"};

		for (int i = 0; i < vet.length; i++)
			if (regex.matcher(vet[i]).find())
				System.err.println(i);
	}
}
