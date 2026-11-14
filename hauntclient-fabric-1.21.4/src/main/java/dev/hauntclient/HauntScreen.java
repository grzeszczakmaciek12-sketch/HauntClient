package dev.hauntclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class HauntScreen extends Screen {
    private static final int PURPLE = 0xFF9B5CFF, PANEL = 0xF0181022, CARD = 0xFF291B38, MUTED = 0xFFB9A8C9;
    private HauntModules.Category selected = HauntModules.Category.HUD;

    public HauntScreen() { super(Text.literal("HAUNT CLIENT V2")); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int w = 470, h = 310, x = (width-w)/2, y = (height-h)/2;
        ctx.fill(x-2,y-2,x+w+2,y+h+2,PURPLE);
        ctx.fill(x,y,x+w,y+h,PANEL);
        ctx.fill(x,y,x+w,y+48,0xFF21132E);
        ctx.drawText(textRenderer,"HAUNT CLIENT V2",x+16,y+10,0xFFFFFFFF,true);
        ctx.drawText(textRenderer,"DEMO MODE  •  FABRIC 1.21.4",x+16,y+27,MUTED,false);
        int tx=x+14;
        for (HauntModules.Category c:HauntModules.Category.values()) {
            ctx.fill(tx,y+58,tx+93,y+82,c==selected?0xFF7040A0:CARD);
            ctx.drawCenteredTextWithShadow(textRenderer,c.name(),tx+46,y+66,c==selected?0xFFFFFFFF:MUTED);
            tx+=98;
        }
        ctx.fill(x+14,y+91,x+w-14,y+116,0xFF100B17);
        ctx.drawText(textRenderer,"LMB: toggle module  •  RMB on X-Ray: choose blocks",x+23,y+100,MUTED,false);
        List<HauntModules.Module> modules=visibleModules();
        for(int i=0;i<modules.size();i++){
            HauntModules.Module m=modules.get(i); int top=y+126+i*37;
            ctx.fill(x+14,top,x+w-14,top+31,CARD);
            ctx.drawText(textRenderer,m.title,x+24,top+5,0xFFFFFFFF,false);
            ctx.drawText(textRenderer,m.description,x+24,top+17,MUTED,false);
            if(m==HauntModules.Module.XRAY) ctx.drawText(textRenderer,"BLOCKS: "+HauntModules.getXrayBlockCount()+"  •  RMB: CONFIGURE",x+250,top+17,0xFFD7B6FF,false);
            int bx=x+w-62; boolean on=HauntModules.isEnabled(m);
            ctx.fill(bx,top+7,bx+36,top+24,on?0xFF8E4DCE:0xFF51465A);
            ctx.fill(on?bx+21:bx+3,top+9,on?bx+33:bx+15,top+22,0xFFFFFFFF);
        }
        ctx.drawText(textRenderer,"SERVER DEMO: Fly requires server permission; radar sees sent blocks only",x+15,y+h-17,MUTED,false);
        super.render(ctx,mouseX,mouseY,delta);
    }

    private List<HauntModules.Module> visibleModules(){
        List<HauntModules.Module> list=new ArrayList<>();
        for(HauntModules.Module m:HauntModules.Module.values()) if(m.category==selected) list.add(m);
        return list;
    }

    @Override
    public boolean mouseClicked(double mx,double my,int button){
        if(button==0){
            int w=470,h=310,x=(width-w)/2,y=(height-h)/2,tx=x+14;
            for(HauntModules.Category c:HauntModules.Category.values()){
                if(mx>=tx&&mx<=tx+93&&my>=y+58&&my<=y+82){selected=c;return true;}
                tx+=98;
            }
            List<HauntModules.Module> list=visibleModules();
            for(int i=0;i<list.size();i++){
                int top=y+126+i*37;
                if(mx>=x+14&&mx<=x+w-14&&my>=top&&my<=top+31){HauntModules.toggle(list.get(i));return true;}
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean shouldPause(){return false;}
}
