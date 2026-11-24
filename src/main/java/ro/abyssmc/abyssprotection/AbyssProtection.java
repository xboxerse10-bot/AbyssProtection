package ro.abyssmc.abyssprotection;
import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.util.*;
import static net.minecraft.commands.Commands.*;
public class AbyssProtection implements ModInitializer {
 static final String PREFIX="&#00D6FF&lᴀ&#00D0FF&lʙ&#00CBFF&lʏ&#00C5FF&lꜱ&#00BFFF&lꜱ&#00B9FF&lᴘ&#00B2FF&lʀ&#00ACFF&lᴏ&#00A5FF&lᴛ&#009EFF&lᴇ&#0097FF&lᴄ&#0090FF&lᴛ&#0089FF&lɪ&#0081FF&lᴏ&#0079FF&lɴ";
 static final String[] FLAGS={"block-break","block-place","containers","interact","pvp","tnt","creeper","other-explosion","fire","liquid","animal-damage"};
 static final Path FILE=Path.of("config/abyssprotection/data.json"); static final Gson G=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

 static final Path MESSAGE_FILE=Path.of("config/abyssprotection/message.yml");
 static final Map<String,String> DEFAULT_MESSAGES=new LinkedHashMap<>();
 static Map<String,String> messages=new LinkedHashMap<>();
 static {DEFAULT_MESSAGES.put("prefix",PREFIX);
  DEFAULT_MESSAGES.put("invalid","&cProtectie invalida sau deja activa.");
  DEFAULT_MESSAGES.put("limit","&cAi atins limita de protectii.");
  DEFAULT_MESSAGES.put("overlap","&cPozitie ocupata sau regiune suprapusa.");
  DEFAULT_MESSAGES.put("activated","&aProtectie activata.");
  DEFAULT_MESSAGES.put("recover-denied","&cNu poti recupera protectia.");
  DEFAULT_MESSAGES.put("recovered","&aProtectie recuperata.");
  DEFAULT_MESSAGES.put("offline","&cJucatorul trebuie sa fie online.");
  DEFAULT_MESSAGES.put("invalid-size","&cDimensiune invalida.");
  DEFAULT_MESSAGES.put("not-owner","&cAceasta nu este protectia ta.");
  DEFAULT_MESSAGES.put("gui-members","&bMembri");
  DEFAULT_MESSAGES.put("gui-flags","&bSetari");
  DEFAULT_MESSAGES.put("gui-allow"," &aACTIVAT");
  DEFAULT_MESSAGES.put("gui-deny"," &cDEZACTIVAT");
  DEFAULT_MESSAGES.put("gui-remove"," &c(click pentru eliminare)");
  DEFAULT_MESSAGES.put("gui-prev","&bAnterior");
  DEFAULT_MESSAGES.put("gui-next","&bUrmator");
  DEFAULT_MESSAGES.put("gui-back","&bInapoi");
  DEFAULT_MESSAGES.put("menu-main","Administrare");
  DEFAULT_MESSAGES.put("menu-flags","Setari");
  DEFAULT_MESSAGES.put("menu-members","Membri");
  DEFAULT_MESSAGES.put("item-name","&#00D6FF&lProtectie {size}");
  DEFAULT_MESSAGES.put("issued","&aAi oferit {amount} blocuri de protectie.");
  DEFAULT_MESSAGES.put("flag-block-break","Spargere blocuri");
  DEFAULT_MESSAGES.put("flag-block-place","Plasare blocuri");
  DEFAULT_MESSAGES.put("flag-containers","Containere");
  DEFAULT_MESSAGES.put("flag-interact","Interactiuni");
  DEFAULT_MESSAGES.put("flag-pvp","PvP");
  DEFAULT_MESSAGES.put("flag-tnt","TNT");
  DEFAULT_MESSAGES.put("flag-creeper","Creeper");
  DEFAULT_MESSAGES.put("flag-other-explosion","Alte explozii");
  DEFAULT_MESSAGES.put("flag-fire","Foc");
  DEFAULT_MESSAGES.put("flag-liquid","Lichide");
  DEFAULT_MESSAGES.put("flag-animal-damage","Daune animale");
  DEFAULT_MESSAGES.put("unknown-owner","Necunoscut");
  DEFAULT_MESSAGES.put("enter-title",PREFIX);
  DEFAULT_MESSAGES.put("exit-title",PREFIX);
  DEFAULT_MESSAGES.put("enter-subtitle","&aAi intrat in protectia lui &f&l{owner}");
  DEFAULT_MESSAGES.put("exit-subtitle","&cAi iesit din protectia lui &f&l{owner}");
  DEFAULT_MESSAGES.put("reload","&aMesajele au fost reincarcate.");
  DEFAULT_MESSAGES.put("reload-failed","&cNu s-au putut reincarca mesajele.");
 }
 static String tr(String key){return messages.getOrDefault(key,DEFAULT_MESSAGES.getOrDefault(key,key));}
 static boolean loadMessages(){try{
  Files.createDirectories(MESSAGE_FILE.getParent());
  if(!Files.exists(MESSAGE_FILE)){StringBuilder out=new StringBuilder("# Mesaje AbyssProtection - foloseste valori intre ghilimele duble.\n");for(var e:DEFAULT_MESSAGES.entrySet())out.append(e.getKey()).append(": ").append(G.toJson(e.getValue())).append('\n');Files.writeString(MESSAGE_FILE,out.toString());}
  Map<String,String> next=new LinkedHashMap<>(DEFAULT_MESSAGES);
  for(String line:Files.readAllLines(MESSAGE_FILE)){String t=line.trim();if(t.isEmpty()||t.startsWith("#"))continue;int i=t.indexOf(':');if(i<1)throw new IllegalArgumentException("Linie invalida: "+t);String key=t.substring(0,i).trim(),value=t.substring(i+1).trim();if(!value.startsWith("\"")||!value.endsWith("\""))throw new IllegalArgumentException("Valoare fara ghilimele: "+key);next.put(key,G.fromJson(value,String.class));}
  messages=next;return true;
 }catch(Exception e){System.err.println("AbyssProtection message.yml: "+e.getMessage());return false;}}
 static Data data=new Data(); static Map<UUID,String> last=new HashMap<>(); static boolean internal=false;
 static class Data {Map<String,Token> tokens=new HashMap<>(); Map<String,Claim> claims=new HashMap<>();}
 static class Token {int x,z; Token(int x,int z){this.x=x;this.z=z;}}
 static class Claim {String id,dim,name,ownerName; UUID owner; int x,y,z,minX,maxX,minZ,maxZ; boolean hidden; Map<UUID,String> members=new LinkedHashMap<>(); Map<String,Boolean> flags=new HashMap<>(); BlockPos pos(){return new BlockPos(x,y,z);} boolean contains(String d,BlockPos p){return dim.equals(d)&&p.getX()>=minX&&p.getX()<=maxX&&p.getZ()>=minZ&&p.getZ()<=maxZ;} boolean allow(String f){return flags.getOrDefault(f,f.equals("fire")||f.equals("liquid"));} }
 static void save(){try{Files.createDirectories(FILE.getParent());Path tmp=FILE.resolveSibling("data.tmp");Files.writeString(tmp,G.toJson(data));Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new IllegalStateException("AbyssProtection save failed",e);}}
 static String dim(ServerLevel l){return l.dimension().identifier().toString();} static String dim(ServerPlayer p){return p.level().dimension().identifier().toString();}
 static Claim at(String d,BlockPos p){for(Claim c:data.claims.values())if(c.contains(d,p))return c;return null;}
 static boolean op(CommandSourceStack s){return s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN);}
 static Object perms(ServerPlayer p)throws Exception{Object api=Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);Object m=Class.forName("net.luckperms.api.LuckPerms").getMethod("getUserManager").invoke(api);Object u=Class.forName("net.luckperms.api.model.user.UserManager").getMethod("getUser",UUID.class).invoke(m,p.getUUID());Object c=Class.forName("net.luckperms.api.model.PermissionHolder").getMethod("getCachedData").invoke(u);return Class.forName("net.luckperms.api.cacheddata.CachedDataManager").getMethod("getPermissionData").invoke(c);}
 static boolean has(ServerPlayer p,String n){try{return "TRUE".equals(String.valueOf(Class.forName("net.luckperms.api.cacheddata.CachedPermissionData").getMethod("checkPermission",String.class).invoke(perms(p),n)));}catch(Exception e){return false;}}
 static boolean access(CommandSourceStack s,String n){return op(s)||s.getEntity() instanceof ServerPlayer p&&(has(p,"abyssprotection.admin")||has(p,"abyssprotection."+n));}
 static boolean admin(ServerPlayer p){return access(p.createCommandSourceStack(),"admin");} static boolean manage(ServerPlayer p,Claim c){return c!=null&&(c.owner.equals(p.getUUID())||admin(p));}
 static int limit(ServerPlayer p){if(admin(p)||has(p,"abyssprotection.limit.unlimited"))return Integer.MAX_VALUE;int n=1;try{Map<?,?> map=(Map<?,?>)Class.forName("net.luckperms.api.cacheddata.CachedPermissionData").getMethod("getPermissionMap").invoke(perms(p));for(Object k:map.keySet()){String s=k.toString();if(s.startsWith("abyssprotection.limit.")&&Boolean.TRUE.equals(map.get(k)))try{n=Math.max(n,Integer.parseInt(s.substring("abyssprotection.limit.".length())));}catch(NumberFormatException ignored){}}}catch(Exception ignored){}return n;}
 static long count(UUID u){return data.claims.values().stream().filter(c->c.owner.equals(u)).count();}
 static void msg(ServerPlayer p,String s){p.sendSystemMessage(HexText.parse(tr("prefix")+" &8» &r"+s));} static int reply(CommandSourceStack s,String m){s.sendSuccess(()->HexText.parse(tr("prefix")+" &8» &r"+m),false);return 1;}
 static ItemStack item(String id){Token t=data.tokens.get(id);ItemStack s=new ItemStack(Items.DIAMOND_BLOCK);s.set(DataComponents.CUSTOM_NAME,HexText.parse(tr("item-name").replace("{size}",t.x+"x"+t.z)));s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);s.set(DataComponents.MAX_STACK_SIZE,1);CompoundTag tag=new CompoundTag();tag.putString("abyssprotection_id",id);CustomData.set(DataComponents.CUSTOM_DATA,s,tag);return s;}
 static String token(ItemStack s){CustomData d=s.get(DataComponents.CUSTOM_DATA);return d==null?null:d.copyTag().getString("abyssprotection_id").orElse(null);}
 static boolean free(ServerPlayer p){for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())return true;return false;}
 static boolean overlap(Claim a,Claim b){return a.dim.equals(b.dim)&&a.minX<=b.maxX&&a.maxX>=b.minX&&a.minZ<=b.maxZ&&a.maxZ>=b.minZ;}
 static boolean guard(Claim c){if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("abyssguard"))return false;try{Object manager=Class.forName("ro.abyssmc.abyssguard.AbyssGuard").getField("REGIONS").get(null);for(Object r:(Collection<?>)manager.getClass().getMethod("all").invoke(manager)){Class<?> k=r.getClass();if(c.dim.equals(k.getField("dimension").get(r))&&c.minX<=k.getField("maxX").getInt(r)&&c.maxX>=k.getField("minX").getInt(r)&&c.minZ<=k.getField("maxZ").getInt(r)&&c.maxZ>=k.getField("minZ").getInt(r))return true;}return false;}catch(Exception e){return true;}}
 static boolean permitted(ServerPlayer p,BlockPos pos,String f){Claim c=at(dim(p),pos);return c==null||manage(p,c)||has(p,"abyssprotection.bypass")||c.members.containsKey(p.getUUID())||c.allow(f);}
 static boolean set(ServerLevel l,BlockPos p,boolean show){internal=true;try{return l.setBlock(p,show?Blocks.DIAMOND_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}finally{internal=false;}}
 static InteractionResult place(ServerPlayer p,ServerLevel l,BlockPos pos,ItemStack stack,String id){Token t=data.tokens.get(id);if(t==null||data.claims.containsKey(id)||!stack.is(Items.DIAMOND_BLOCK)){msg(p,tr("invalid"));return InteractionResult.FAIL;}if(count(p.getUUID())>=limit(p)){msg(p,tr("limit"));return InteractionResult.FAIL;}Claim c=new Claim();c.id=id;c.owner=p.getUUID();c.name=p.getName().getString();c.ownerName=c.name;c.dim=dim(l);c.x=pos.getX();c.y=pos.getY();c.z=pos.getZ();long x=(long)c.x-t.x/2,z=(long)c.z-t.z/2;if(x< -30000000||z< -30000000||x+t.x>30000001||z+t.z>30000001)return InteractionResult.FAIL;c.minX=(int)x;c.minZ=(int)z;c.maxX=(int)(x+t.x-1);c.maxZ=(int)(z+t.z-1);if(!l.getBlockState(pos).isAir()||guard(c)||data.claims.values().stream().anyMatch(a->overlap(a,c))){msg(p,tr("overlap"));return InteractionResult.FAIL;}if(!set(l,pos,true))return InteractionResult.FAIL;data.claims.put(id,c);stack.shrink(1);save();msg(p,tr("activated"));return InteractionResult.SUCCESS;}
 public void onInitialize(){loadMessages();try{if(Files.exists(FILE))data=G.fromJson(Files.readString(FILE),Data.class);else save();}catch(Exception e){throw new IllegalStateException(e);}
 UseBlockCallback.EVENT.register((player,w,h,hit)->{if(!(player instanceof ServerPlayer p)||!(w instanceof ServerLevel l))return InteractionResult.PASS;BlockPos pos=hit.getBlockPos();Claim c=at(dim(p),pos);if(c!=null&&c.pos().equals(pos)){if(manage(p,c))menu(p,c,0,0);return InteractionResult.SUCCESS;}ItemStack s=p.getItemInHand(h);String id=token(s);if(id!=null)return place(p,l,pos.relative(hit.getDirection()),s,id);if(!permitted(p,pos,"interact")||!permitted(p,pos,"containers")||!permitted(p,pos.relative(hit.getDirection()),"block-place"))return InteractionResult.FAIL;return InteractionResult.PASS;});
 PlayerBlockBreakEvents.BEFORE.register((w,player,pos,state,be)->{if(!(player instanceof ServerPlayer p)||!(w instanceof ServerLevel l))return true;Claim c=at(dim(p),pos);if(c!=null&&c.pos().equals(pos)){if(!manage(p,c)||!free(p)){msg(p,tr("recover-denied"));return false;}if(set(l,pos,false)){data.claims.remove(c.id);p.getInventory().add(item(c.id));save();msg(p,tr("recovered"));}return false;}return permitted(p,pos,"block-break");});
 ServerLivingEntityEvents.ALLOW_DAMAGE.register((e,s,a)->{if(!(e.level() instanceof ServerLevel l))return true;Claim c=at(dim(l),e.blockPosition());if(c==null)return true;if(e instanceof ServerPlayer&&s.getEntity() instanceof ServerPlayer)return c.allow("pvp");if(!(e instanceof ServerPlayer)&&s.getEntity() instanceof ServerPlayer p)return permitted(p,e.blockPosition(),"animal-damage");return true;});
 ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerPlayer p:server.getPlayerList().getPlayers()){Claim c=at(dim(p),p.blockPosition());String id=c==null?"":c.id;String old=last.put(p.getUUID(),id);if(!id.equals(old)&&(c!=null||old!=null&&!old.isEmpty())){Claim previous=old==null?null:data.claims.get(old);Claim shown=c!=null?c:previous;String name=shown==null?tr("unknown-owner"):shown.ownerName!=null?shown.ownerName:shown.name; if(shown!=null)for(ServerPlayer online:server.getPlayerList().getPlayers())if(online.getUUID().equals(shown.owner)){name=online.getName().getString();if(!name.equals(shown.ownerName)){shown.ownerName=name;save();}break;}p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5,45,10));p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(HexText.parse(tr(c==null?"exit-subtitle":"enter-subtitle").replace("{owner}",name))));p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(HexText.parse(tr(c==null?"exit-title":"enter-title"))));}}});
 CommandRegistrationCallback.EVENT.register((d,r,e)->{var root=literal("ap");root.then(literal("reload").requires(src->access(src,"reload")).executes(ctx->reply(ctx.getSource(),tr(loadMessages()?"reload":"reload-failed"))));root.then(literal("give").requires(s->access(s,"protection.give")).then(argument("player",StringArgumentType.word()).then(argument("size",StringArgumentType.word()).executes(ctx->give(ctx.getSource(),StringArgumentType.getString(ctx,"player"),StringArgumentType.getString(ctx,"size"),1)).then(argument("amount",IntegerArgumentType.integer(1,36)).executes(ctx->give(ctx.getSource(),StringArgumentType.getString(ctx,"player"),StringArgumentType.getString(ctx,"size"),IntegerArgumentType.getInteger(ctx,"amount")))))));
 for(String cmd:List.of("settings","info","members","list","hide","show"))root.then(literal(cmd).executes(ctx->action(ctx.getSource(),cmd,"")));for(String cmd:List.of("add","remove","rename","transfer"))root.then(literal(cmd).then(argument("value",StringArgumentType.greedyString()).executes(ctx->action(ctx.getSource(),cmd,StringArgumentType.getString(ctx,"value")))));var node=d.register(root);d.register(literal("abyssprotection").redirect(node));}); }
 static int give(CommandSourceStack s,String name,String size,int amount){try{String[] a=size.toLowerCase(Locale.ROOT).split("x",-1);if(a.length>2)return 0;int x=Integer.parseInt(a[0]),z=a.length==1?x:Integer.parseInt(a[1]);if(x<1||z<1||x>60000000||z>60000000)return 0;ServerPlayer p=s.getServer().getPlayerList().getPlayerByName(name);if(p==null)return reply(s,tr("offline"));int n=0;while(n<amount&&free(p)){String id=UUID.randomUUID().toString();data.tokens.put(id,new Token(x,z));save();p.getInventory().add(item(id));n++;}return reply(s,tr("issued").replace("{amount}",String.valueOf(n)));}catch(NumberFormatException e){return reply(s,tr("invalid-size"));}}
 static int action(CommandSourceStack s,String cmd,String v){if(!(s.getEntity() instanceof ServerPlayer p))return 0;if(cmd.equals("list"))return reply(s,data.claims.values().stream().filter(c->manage(p,c)).map(c->c.name+" "+c.pos()).toList().toString());Claim c=at(dim(p),p.blockPosition());if(!manage(p,c))return reply(s,tr("not-owner"));switch(cmd){case "settings"->menu(p,c,0,0);case "info"->msg(p,c.name+" "+c.pos()+" "+c.id);case "members"->menu(p,c,2,0);case "rename"->{if(v.length()>40)return 0;c.name=v;}case "hide","show"->{boolean show=cmd.equals("show");if(show==!c.hidden)return 1;ServerLevel l=(ServerLevel)p.level();if(show&&!l.getBlockState(c.pos()).isAir())return 0;if(!set(l,c.pos(),show))return 0;c.hidden=!show;}case "add","transfer"->{ServerPlayer target=s.getServer().getPlayerList().getPlayerByName(v);if(target==null)return reply(s,tr("offline"));if(cmd.equals("add"))c.members.put(target.getUUID(),target.getName().getString());else{if(count(target.getUUID())>=limit(target))return 0;c.owner=target.getUUID();c.ownerName=target.getName().getString();c.members.remove(c.owner);}}case "remove"->c.members.entrySet().removeIf(e->e.getValue().equalsIgnoreCase(v));}save();return 1;}
 static ItemStack icon(Item i,String name){ItemStack s=new ItemStack(i);s.set(DataComponents.CUSTOM_NAME,HexText.parse(name));return s;}
 static void menu(ServerPlayer p,Claim c,int tab,int page){SimpleContainer inv=new SimpleContainer(54);List<UUID> members=new ArrayList<>(c.members.keySet());if(tab==0){inv.setItem(11,icon(Items.PLAYER_HEAD,tr("gui-members")));inv.setItem(13,icon(Items.REDSTONE,tr("gui-flags")));inv.setItem(15,icon(Items.BOOK,"&b"+c.name));}else if(tab==1){for(int i=0;i<FLAGS.length;i++)inv.setItem(i,icon(c.allow(FLAGS[i])?Items.EMERALD:Items.REDSTONE,"&b"+tr("flag-"+FLAGS[i])+(c.allow(FLAGS[i])?tr("gui-allow"):tr("gui-deny"))));}else{for(int i=0;i<45&&page*45+i<members.size();i++)inv.setItem(i,icon(Items.PLAYER_HEAD,"&b"+c.members.get(members.get(page*45+i))+tr("gui-remove")));inv.setItem(45,icon(Items.ARROW,tr("gui-prev")));inv.setItem(53,icon(Items.ARROW,tr("gui-next")));}inv.setItem(49,icon(Items.ARROW,tr("gui-back")));p.openMenu(new SimpleMenuProvider((id,pi,who)->new ChestMenu(MenuType.GENERIC_9x6,id,pi,inv,6){@Override public void clicked(int slot,int button,ContainerInput type,Player who){if(!manage(p,c)||data.claims.get(c.id)!=c)return;if(slot==49){menu(p,c,0,0);return;}if(tab==0){if(slot==11)menu(p,c,2,0);if(slot==13)menu(p,c,1,0);}else if(tab==1&&slot>=0&&slot<FLAGS.length){String f=FLAGS[slot];c.flags.put(f,!c.allow(f));save();menu(p,c,1,0);}else if(tab==2){if(slot==45&&page>0)menu(p,c,2,page-1);else if(slot==53&&(page+1)*45<members.size())menu(p,c,2,page+1);else if(slot>=0&&slot<45&&page*45+slot<members.size()){c.members.remove(members.get(page*45+slot));save();menu(p,c,2,0);}}} @Override public boolean stillValid(Player who){return manage(p,c)&&data.claims.get(c.id)==c;} @Override public ItemStack quickMoveStack(Player who,int slot){return ItemStack.EMPTY;}},HexText.parse(tr("prefix")+" &8| &b"+(tab==0?tr("menu-main"):tab==1?tr("menu-flags"):tr("menu-members")))));}
 public static boolean worldChange(ServerLevel l,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){if(internal)return true;Claim c=at(dim(l),pos);if(c==null)return true;if(c.pos().equals(pos))return false;if(state.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock)return c.allow("fire");if(!state.getFluidState().isEmpty()&&!c.allow("liquid"))return false;return true;}
 public static boolean explosion(ServerLevel l,BlockPos p,String f){for(Claim c:data.claims.values())if(c.dim.equals(dim(l))&&p.getX()+32L>=c.minX&&p.getX()-32L<=c.maxX&&p.getZ()+32L>=c.minZ&&p.getZ()-32L<=c.maxZ&&!c.allow(f))return false;return true;}
}
