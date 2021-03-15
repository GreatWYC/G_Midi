
/***G_Midi
* @author GreatWYC
* @version 2.0
*/

package com.greatwyc;

import java.util.*;
import java.io.*;
import java.lang.Number;

public class G_Midi
{
	public static List<List> parseMidi(File f) throws Exception{
		List<List> result = new ArrayList<List>();
		List<int[]> MTrk = new ArrayList<int[]>();
		int[] MThd = new int[6];
		InputStream fs = new FileInputStream(f);
		int size = fs.available();
		int[] data = new int[size];
		int index = 0;
		
		//文件读取
		for(int i=0;fs.available()>0;i++){
			data[i] = fs.read();
		}
		fs.close();
		
		
		//Midi数据分块
		while(index != size ){
			if(index+1 < size && index+2 < size && index+3 < size && index+4 < size){
				if(data[index]==77&&data[index+1]==84&&data[index+2]==104&&data[index+3]==100){
					MThd = slice(data,index+8,index+8+5);
					index += 14;
				}
				if(data[index]==77&&data[index+1]==84&&data[index+2]==114&&data[index+3]==107){
					MTrk.add(slice(data,index+8,(int) (index + 8 + data[index+4]*Math.pow(16,6) + data[index+5]*Math.pow(16,4) + data[index+6]*Math.pow(16,2) + data[index+7] - 1)));
					index += (int) (8 + data[index+4]*Math.pow(16,6) + data[index+5]*Math.pow(16,4) + data[index+6]*Math.pow(16,2) + data[index+7]);
				}
			}
		}
		
		//MTrk处理
		for(int i = 0; i < MTrk.size();i++){
			result.add(spliter(MTrk.get(i)));
		}
		
		return result;
	}
	
	
	//MTrk块解析方法
	private static List<Map> spliter(int[] MTrk_block){
		int index = 0;
		int mode = 0;
		List<Map> result = new ArrayList<Map>();
		while(index < MTrk_block.length - 1){
			Map m = new HashMap();
			int r[] = action_bytes_reader(MTrk_block,index);
			m.put("delta_time",r[0]);
			index += r[1];
			if(MTrk_block[index]<128&&index<(MTrk_block.length - 1)){
				String lastEvent = (String) result.get(result.size()-1).get("event");
				m.put("event",lastEvent);
				m.put("channel",result.get(result.size()-2).get("channel"));
				if(lastEvent.equals("note_on")||lastEvent.equals("note_off")||lastEvent.equals("polyphonic_aftertouch")){
					m.put("note",MTrk_block[index]);
					m.put("velocity",MTrk_block[index+1]);
					index+=2;
				}else if(lastEvent.equals("control_mode_change")){
					m.put("number",MTrk_block[index]);
					m.put("value",MTrk_block[index+1]);
					index+=2;
				}
			}else if(MTrk_block[index]%144<16 && (int) (MTrk_block[index]/144)==1){
				m.put("event","note_on");
				m.put("channel",MTrk_block[index]%144);
				m.put("note",MTrk_block[index+1]);
				m.put("velocity",MTrk_block[index+2]);
				index+=3;
			}else if(MTrk_block[index]%128<16 && (int) (MTrk_block[index]/128)==1){
				m.put("event","note_off");
				m.put("channel",MTrk_block[index]%128);
				m.put("note",MTrk_block[index+1]);
				m.put("velocity",MTrk_block[index+2]);
				index+=3;
			}else if(MTrk_block[index]%160<16 && (int) (MTrk_block[index]/160)==1){
				m.put("event","polyphonic_aftertouch");
				m.put("channel",MTrk_block[index]%160);
				m.put("note",MTrk_block[index+1]);
				m.put("velocity",MTrk_block[index+2]);
				index+=3;
			}else if(MTrk_block[index]%176<16 && (int) (MTrk_block[index]/176)==1){
				m.put("event","control_mode_change");
				m.put("channel",MTrk_block[index]%176);
				m.put("number",MTrk_block[index+1]);
				m.put("value",MTrk_block[index+2]);
				index+=3;
			}else if(MTrk_block[index]%192<16 && (int) (MTrk_block[index]/192)==1){
				m.put("event","program_change");
				m.put("channel",MTrk_block[index]%192);
				m.put("number",MTrk_block[index+1]);
				index+=2;
			}else if(MTrk_block[index]%208<16 && (int) (MTrk_block[index]/208)==1){
				m.put("event","channel_after_touch");
				m.put("channel",MTrk_block[index]%208);
				m.put("amount",MTrk_block[index+1]);
				index+=2;
			}else if(MTrk_block[index]%224<16 && (int) (MTrk_block[index]/224)==1){
				m.put("event","pitch_wheel_control");
				m.put("channel",MTrk_block[index]%224);
				m.put("LSB",MTrk_block[index+1]);
				m.put("MSB",MTrk_block[index+2]);
				index+=3;
			}else if(MTrk_block[index]==255){
				
				m.put("event","meta_event");
				
				if(MTrk_block[index+1]==0){
					m.put("type","sequence_number");
					m.put("number",slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==1){
					m.put("type","text");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==2){
					m.put("type","copyright");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==3){
					m.put("type","track_name");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==4){
					m.put("type","instrument_name");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==5){
					m.put("type","lyric");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==6){
					m.put("type","marker");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==7){
					m.put("type","cue_point");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==8){
					m.put("type","program_name");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==9){
					m.put("type","device_name");
					m.put("text",ArrayToString(slice(MTrk_block,index+3,index+MTrk_block[index+2]+3-1)));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==32){
					m.put("type","channel_prefix");
					m.put("channel",MTrk_block[index+3]);
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==33){
					m.put("type","midi_port");
					m.put("port",MTrk_block[index+3]);
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==47){
					m.put("type","end_of_track");
					index+=2;
				}else if(MTrk_block[index+1]==81){
					m.put("type","tempo");
					m.put("time",60000000/(MTrk_block[index+3]*Math.pow(16,4)+MTrk_block[index+4]*Math.pow(16,2)+MTrk_block[index+5]));
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==84){
					m.put("type","SMPTE_offset");
					m.put("hour",MTrk_block[index+3]);
					m.put("minute",MTrk_block[index+4]);
					m.put("second",MTrk_block[index+5]);
					m.put("frame",MTrk_block[index+6]);
					m.put("fractional_frame",MTrk_block[index+7]);
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==88){
					m.put("type","time_signature");
					m.put("time_signature",(int) MTrk_block[index+3] + "/" + (int) Math.pow(MTrk_block[index+4],2));
					m.put("clock",MTrk_block[index+5]);
					m.put("bb",MTrk_block[index+6]);
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==89){
					m.put("type","key_signature");
					m.put("sf",MTrk_block[index+3]);
					m.put("mi",MTrk_block[index+4]);
					index+=MTrk_block[index+2]+3;
				}else if(MTrk_block[index+1]==127){
					m.put("type","sequencer_specific");
					m.put("data",slice(MTrk_block,index,index+MTrk_block[index+2]+3-1));
					index+=MTrk_block[index+2]+3;
				}else{
					m.put("type","unknow_meta_event");
					index+=1;
				}
			}else{
				m.put("event","unknow_event");
				index+=1;
			}
			
			result.add(m);
			continue;
		}
		return result;
	}
	
	//动态字节读取方法
	private static int[] action_bytes_reader(int[] data,int index){
		int result = 0;
		int next_index = 0;
		List<Integer> action_bytes = new ArrayList<Integer>();

		while(next_index == 0 || data[index+next_index-1] > 127){
			action_bytes.add(data[index+next_index]-(data[index+next_index]>127?128:0));
			next_index++;
		}
		for(int i = 0;i < next_index;i++){
			result += action_bytes.get(i).intValue()*Math.pow(128,next_index-i-1);
		}

		return new int[]{result,next_index};
	}
	
	//整型数组转字符串方法
	private static String ArrayToString(int[] data){
		StringBuffer result = new StringBuffer();
		for(int i = 0;i < data.length;i++){
			result.append((char) data[i]);
		}
		return result.toString();
	};
	
	//数组分割方法
	//注意使用时index1减一
	private static int[] slice(int[] arr,int index0,int index1){
		int[] result = new int[index1 - index0 + 1];
		for(int i = 0;i < index1-index0 + 1;i++){
			result[i] = arr[index0+i];
		}
		return result;
	}
	
}
