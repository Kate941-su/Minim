package ddf.minim.ugens;

import java.util.Vector;

import ddf.minim.AudioOutput;
import ddf.minim.AudioSignal;
import ddf.minim.Minim;

/**
 * A UGen that will group together other UGens and sum their output.
 * @author ddf
 *
 */
public class Summer extends UGen implements AudioSignal
{
	private AudioOutput out;
	private Vector<UGen> ugens;
	
	public Summer()
	{
		ugens = new Vector<UGen>();
	}
	
	// ddf: I do not expect users of the library to construct busses that they pass their output to.
	//      This exists so that we can tick the noteManager for the provided output.
	//      In other words, the output passed to this constructor will always be the same output
	//      that is constructing the bus. However, because AudioOutput is in a different package,
	//      this must be a public constructor. :-/
	public Summer(AudioOutput output)
	{
		out = output;
		ugens = new Vector<UGen>();
	}
	
	// ddf: override because everything that patches to us
	//      goes into our list. then when we generate a sample
	//      we'll sum the audio generated by all of the ugens patched to us.
	@Override
	protected void addInput(UGen input)
	{
		Minim.debug("Bus::addInput - Adding " + input + " to the ugens list of " + this);
		ugens.add(input);
	}
	
	@Override
	protected void removeInput(UGen input)
	{
		Minim.debug("Bus::removeInput - Removing " + input + " to the ugens list of " + this);
		ugens.remove(input);
	}
	
	protected void sampleRateChanged()
	{
		// ddf: need to let all of the UGens in our list know about the sample rate change
		for(int i = 0; i < ugens.size(); i++)
		{
			ugens.get(i).setSampleRate(sampleRate());
		}
	}
	
	@Override
	protected void uGenerate(float[] channels) 
	{
		// ddf: we use toArray here because it's possible that one of the
		//      UGens in our list will remove itself from the list as part of
		//      its tick (for example: ADSR has an unpatchAfterNoteFinished feature
		//      which results in it unpatching itself during its uGenerate call).
		//      If the list is modified while we are iterating over it, we won't 
		//      generate audio correctly.
		UGen[] ugensArray = ugens.toArray( new UGen[] {} );
		for(int i = 0; i < ugensArray.length; i++)
		{
			float[] tmp = new float[channels.length];
			UGen u = ugensArray[i];
			u.tick(tmp);
			for(int c = 0; c < channels.length; c++)
			{
				channels[c] += tmp[c];
			}
		}
	}
	
	/**
	 * Generates a buffer of samples by ticking this UGen mono.length times. Like the 
	 * tick method, this will result in all of the 
	 */
	public void generate(float[] mono)
	{
		float[] sample = new float[1];
		for(int i = 0; i < mono.length; i++)
		{
			if ( out != null )
			{
				out.noteManager.tick();
			}
			sample[0] = 0;
			tick(sample);
			mono[i] = sample[0];
		}
	}
	
	public void generate(float[] left, float[] right)
	{
		float[] sample = new float[2];
		for(int i = 0; i < left.length; i++)
		{
			if ( out != null )
			{
				out.noteManager.tick();
			}
			sample[0] = 0;
			sample[1] = 0;
			tick(sample);
			left[i] = sample[0];
			right[i] = sample[1];
		}
	}

}
