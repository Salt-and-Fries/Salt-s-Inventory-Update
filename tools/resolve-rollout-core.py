"""Reviewed resolution of the rollout's known cross-version Core conflicts.

Consumes saved merge proposals, never restarts the initial three-way merge.
Retains each target's rendering backend, codec openers and session authorization.
"""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/all-target-rollout'
BLOCK=re.compile(r'^<<<<<<<.*?\n(.*?)^\|\|\|\|\|\|\|.*?\n(.*?)^=======\n(.*?)^>>>>>>>[^\n]*\n?',re.M|re.S)

def replace_method(text,signature,replacement):
    start=text.index(signature); opening=text.index('{',start); depth=1; end=opening+1
    while depth:
        depth += (text[end]=='{')-(text[end]=='}');end+=1
    return text[:start]+replacement+text[end:]

def main():
    report=json.loads((WORK/'core-merge-report.json').read_text())
    for row in report:
        target=ROOT/row['target']; name=target.name; version=target.parts[target.parts.index('versions')+1]
        if row['status']=='conflict-review':
            # The old sprite-sheet fallback has no equivalent in split-sprite versions.
            if name=='GuiGraphicsExtractor.java':
                row['resolution']='Retained modern native split-sprite renderer';continue
            proposal=(ROOT/row['proposal']).read_text(); index=0
            def resolve(m):
                nonlocal index
                i=index;index+=1;ours,base,theirs=m.groups()
                if name in ('DesktopClientApi.java','DesktopServerApi.java'):return theirs
                if name=='AutomaticContainerSupport.java':return ours
                if name=='NativeFrameBuffer.java':return ours
                if name=='NativeTemplateWindow.java':return ours.replace('!slot.player()','retained(c,slot)')
                if name=='InventoryDesktopScreen.java':
                    if i==0:return ours
                    if i==1:return ours.replace('instanceof MerchantMenu','instanceof DesktopMerchantAccess')
                    return ours+theirs
                if name=='DesktopContainerSessions.java':
                    if i==0:return ours.replace('instanceof MerchantMenu','instanceof DesktopMerchantAccess')
                    if i==1:return ours # Keep modern contextual-recipe helpers; adapt button handler below.
                    if i in (2,3):return theirs
                    return ours+'        private @Nullable byte[] merchantSyncState;\n        private com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks.@Nullable Lease nativeItemSourceLease;\n'
                raise ValueError((name,i))
            text=BLOCK.sub(resolve,proposal)
            assert '<<<<<<<' not in text
            target.write_text(text)
            row['resolution']='Reviewed target-specific resolution'
        elif row['status']=='missing-target-review':
            row['resolution']='Minecraft provides the native extractor on this target; no legacy wrapper added'

    for version in ['1.21.1','1.21.11','26.1.2','26.2','26.3']:
        root=ROOT/f'versions/{version}/fabric/src/main/java/com/salts_inventory_update'
        # Normalize only the files involved in this rollout, preserving all other source.
        for row in report:
            target=ROOT/row['target']
            if f'versions/{version}/' not in row['target'] or target.suffix!='.java' or not target.exists():continue
            text=target.read_text()
            if version!='1.21.1':text=text.replace('ResourceLocation','Identifier')
            cls='ResourceLocation' if version=='1.21.1' else 'Identifier'
            text=re.sub(r'new '+cls+r'\(("[^"\n]*"),\s*("[^"\n]*")\)',cls+r'.fromNamespaceAndPath(\1, \2)',text)
            target.write_text(text)

        target=root/'client/automatic/NativeFrameBuffer.java';text=target.read_text()
        if version=='1.21.1':
            text=text.replace('var mc=context.minecraft();','var mc=context.minecraft();var previousScreen=mc.screen;')
            text=text.replace('state.capturing=true;','state.capturing=true;mc.screen=state.screen;')
            text=text.replace('state.capturing=false;','mc.screen=previousScreen;state.capturing=false;')
        else:
            text=text.replace('state.capturing=true;','var mc=context.minecraft();var previousScreen=mc.screen;\n        state.capturing=true;mc.screen=state.screen;')
            text=text.replace('finally {state.capturing=false;}','finally {mc.screen=previousScreen;state.capturing=false;}')
            scope=root/'client/automatic/AutomaticNativeScope.java';s=scope.read_text()
            s=s.replace('var b=f.owner().buffer;','var b=f.owner().buffer;')
            s=s.replace('Math.max(1,(int)Math.ceil(b.bounds.width()*b.scale)),b.pixelHeight,b.scale','Math.max(1,(int)Math.ceil(f.layout().width()*net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale())),\n            Math.max(1,(int)Math.ceil(f.layout().height()*net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale())),\n            net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale()')
            scope.write_text(s)
        target.write_text(text)

        target=root/'server/DesktopContainerSessions.java';text=target.read_text()
        text=text.replace('instanceof MerchantMenu','instanceof DesktopMerchantAccess')
        methods={'getOffers':'desktopOffers','setSelectionHint':'desktopSelect','tryMoveItems':'desktopMovePayment','getTraderLevel':'desktopLevel','getTraderXp':'desktopXp','showProgressBar':'desktopShowProgress','canRestock':'desktopCanRestock'}
        for old,new in methods.items():text=text.replace('merchantMenu.'+old+'(', 'merchantMenu.'+new+'(')
        # Registry-aware wire codec tracks all native offers and data components.
        text=text.replace('CompoundTag state = offers.createTag();','var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess());\n        byte[] state;\n        try {\n            MerchantOffers.STREAM_CODEC.encode(buffer, offers);\n            buffer.writeVarInt(merchantMenu.desktopLevel());buffer.writeVarInt(merchantMenu.desktopXp());\n            buffer.writeBoolean(merchantMenu.desktopShowProgress());buffer.writeBoolean(merchantMenu.desktopCanRestock());\n            state = new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),state);\n        } finally {buffer.release();}')
        text=re.sub(r'        state\.put(?:Int|Boolean)\("siu[^\n]*\n','',text)
        text=text.replace('state.equals(session.merchantSyncState)','java.util.Arrays.equals(state,session.merchantSyncState)')
        target.write_text(text)

        target=root/'client/InventoryDesktopScreen.java';text=target.read_text()
        cls='ResourceLocation' if version=='1.21.1' else 'Identifier'
        start=text.index('        graphics.unwrap().setColor',text.index('    private void renderMerchantTradeRow('))
        end=text.index('        if (selected)',start)
        text=text[:start]+f'        this.blitSprite(graphics, {cls}.withDefaultNamespace(selected || hovered ? "widget/button_highlighted" : "widget/button"), rowX, rowY, MERCHANT_TRADE_LIST_WIDTH, MERCHANT_TRADE_ROW_HEIGHT);\n'+text[end:]
        # Modern Minecraft's extracted GUI atlas owns the experience-bar artwork.
        text=text.replace('blitRegion(graphics, MERCHANT_TEXTURE, x, y, 0, 186,\n            MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT, MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT, 512, 256);',f'this.blitSprite(graphics, {cls}.withDefaultNamespace("container/villager/experience_bar_background"), x, y, MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT);')
        text=text.replace('blitRegion(graphics, MERCHANT_TEXTURE, x, y, 0, 191,\n            currentWidth + 1, MERCHANT_PROGRESS_HEIGHT, currentWidth + 1, MERCHANT_PROGRESS_HEIGHT, 512, 256);',f'this.blitSprite(graphics, {cls}.withDefaultNamespace("container/villager/experience_bar_current"), MERCHANT_PROGRESS_WIDTH, MERCHANT_PROGRESS_HEIGHT, 0, 0, x, y, currentWidth + 1, MERCHANT_PROGRESS_HEIGHT);')
        text=text.replace('blitRegion(graphics, MERCHANT_TEXTURE, x + currentWidth + 1, y + 1, 2, 182,\n                previewWidth, 3, previewWidth, 3, 512, 256);',f'this.blitSprite(graphics, {cls}.withDefaultNamespace("container/villager/experience_bar_result"), MERCHANT_PROGRESS_WIDTH, 3, 0, 0, x + currentWidth + 1, y + 1, previewWidth, 3);')
        target.write_text(text)
    (WORK/'core-resolution-report.json').write_text(json.dumps(report,indent=2))
    print('Resolved Core proposals and preserved modern renderer/codec implementations.')

if __name__=='__main__':main()
