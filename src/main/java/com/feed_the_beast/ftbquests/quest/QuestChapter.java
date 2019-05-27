package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigString;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.IconAnimation;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.util.ListUtils;
import com.feed_the_beast.ftbquests.events.ObjectCompletedEvent;
import com.feed_the_beast.ftbquests.net.MessageDisplayCompletionToast;
import com.feed_the_beast.ftbquests.util.ConfigQuestObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public final class QuestChapter extends QuestObject
{
	public final QuestFile file;
	public final List<Quest> quests;
	public final List<String> description;
	public boolean alwaysInvisible;
	public QuestChapter group;

	public QuestChapter(QuestFile f)
	{
		file = f;
		description = new ArrayList<>(0);
		quests = new ArrayList<>();
		alwaysInvisible = false;
		group = null;
	}

	@Override
	public QuestObjectType getObjectType()
	{
		return QuestObjectType.CHAPTER;
	}

	@Override
	public QuestFile getQuestFile()
	{
		return file;
	}

	@Override
	public QuestChapter getQuestChapter()
	{
		return this;
	}

	@Override
	public void writeData(NBTTagCompound nbt)
	{
		super.writeData(nbt);

		if (!description.isEmpty())
		{
			NBTTagList list = new NBTTagList();

			for (String v : description)
			{
				list.appendTag(new NBTTagString(v));
			}

			nbt.setTag("description", list);
		}

		if (alwaysInvisible)
		{
			nbt.setBoolean("always_invisible", true);
		}

		if (group != null && !group.invalid)
		{
			nbt.setInteger("group", group.id);
		}
	}

	@Override
	public void readData(NBTTagCompound nbt)
	{
		super.readData(nbt);
		description.clear();

		NBTTagList desc = nbt.getTagList("description", Constants.NBT.TAG_STRING);

		for (int i = 0; i < desc.tagCount(); i++)
		{
			description.add(desc.getStringTagAt(i));
		}

		alwaysInvisible = nbt.getBoolean("always_invisible");
		group = file.getChapter(nbt.getInteger("group"));
	}

	@Override
	public void writeNetData(DataOut data)
	{
		super.writeNetData(data);
		data.writeCollection(description, DataOut.STRING);
		data.writeBoolean(alwaysInvisible);
		data.writeInt(group == null || group.invalid ? 0 : group.id);
	}

	@Override
	public void readNetData(DataIn data)
	{
		super.readNetData(data);
		data.readCollection(description, DataIn.STRING);
		alwaysInvisible = data.readBoolean();
		group = file.getChapter(data.readInt());
	}

	public int getIndex()
	{
		return file.chapters.indexOf(this);
	}

	@Override
	public int getRelativeProgressFromChildren(ITeamData data)
	{
		int progress = 0;

		for (Quest quest : quests)
		{
			if (!quest.canRepeat)
			{
				progress += quest.getRelativeProgress(data);
			}
		}

		return getRelativeProgressFromChildren(progress, quests.size());
	}

	public boolean getOnlyHasRepeatableQuests()
	{
		for (Quest quest : quests)
		{
			if (!quest.canRepeat)
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void onCompleted(ITeamData data, List<EntityPlayerMP> onlineMembers)
	{
		super.onCompleted(data, onlineMembers);
		new ObjectCompletedEvent.ChapterEvent(data, this).post();

		if (!getOnlyHasRepeatableQuests())
		{
			for (EntityPlayerMP player : onlineMembers)
			{
				new MessageDisplayCompletionToast(id).sendTo(player);
			}
		}

		if (file.isComplete(data))
		{
			file.onCompleted(data, onlineMembers);
		}
	}

	@Override
	public void changeProgress(ITeamData data, EnumChangeProgress type)
	{
		for (Quest quest : quests)
		{
			quest.changeProgress(data, type);
		}
	}

	@Override
	public Icon getAltIcon()
	{
		List<Icon> list = new ArrayList<>();

		for (Quest quest : quests)
		{
			if (!quest.getVisibility().isInvisible())
			{
				list.add(quest.getIcon());
			}
		}

		return IconAnimation.fromList(list, false);
	}

	@Override
	public ITextComponent getAltDisplayName()
	{
		return new TextComponentTranslation("ftbquests.unnamed");
	}

	@Override
	public void deleteSelf()
	{
		super.deleteSelf();
		file.chapters.remove(this);
	}

	@Override
	public void deleteChildren()
	{
		for (Quest quest : quests)
		{
			quest.deleteChildren();
			quest.invalid = true;
		}

		quests.clear();
	}

	@Override
	public void onCreated()
	{
		file.chapters.add(this);

		if (!quests.isEmpty())
		{
			for (Quest quest : ListUtils.clearAndCopy(quests))
			{
				quest.onCreated();
			}
		}
	}

	@Override
	public File getFile(File folder)
	{
		return new File(folder, "chapters/" + getCodeString());
	}

	@Override
	public void getConfig(ConfigGroup config)
	{
		super.getConfig(config);
		config.addList("description", description, new ConfigString(""), ConfigString::new, ConfigString::getString);
		config.addBool("always_invisible", () -> alwaysInvisible, v -> alwaysInvisible = v, false);
		config.add("group", new ConfigQuestObject(file, group, QuestObjectType.CHAPTER, QuestObjectType.NULL)
		{
			@Nullable
			@Override
			public QuestObjectBase getObject()
			{
				return group;
			}

			@Override
			public void setObject(@Nullable QuestObjectBase v)
			{
				group = (QuestChapter) v;
			}
		}, new ConfigQuestObject(file, null, QuestObjectType.CHAPTER, QuestObjectType.NULL));
	}

	public boolean isVisible(@Nullable ITeamData data)
	{
		if (alwaysInvisible || quests.isEmpty() && !hasChildren())
		{
			return false;
		}

		if (quests.isEmpty())
		{
			for (QuestChapter child : getChildren())
			{
				if (child.isVisible(data))
				{
					return true;
				}
			}

			return false;
		}

		for (Quest quest : quests)
		{
			if (!quest.getActualVisibility(data).isInvisible())
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public void clearCachedData()
	{
		super.clearCachedData();

		for (Quest quest : quests)
		{
			quest.clearCachedData();
		}
	}

	public boolean hasChildren()
	{
		for (QuestChapter chapter : file.chapters)
		{
			if (chapter.group == this)
			{
				return true;
			}
		}

		return false;
	}

	public List<QuestChapter> getChildren()
	{
		List<QuestChapter> list = Collections.emptyList();

		for (QuestChapter chapter : file.chapters)
		{
			if (chapter.group == this)
			{
				if (list.isEmpty())
				{
					list = new ArrayList<>(3);
				}

				list.add(chapter);
			}
		}

		return list;
	}

	public boolean hasUnclaimedRewards(UUID player, ITeamData data)
	{
		for (Quest quest : quests)
		{
			if (quest.hasUnclaimedRewards(player, data))
			{
				return true;
			}
		}

		for (QuestChapter chapter : getChildren())
		{
			if (chapter.hasUnclaimedRewards(player, data))
			{
				return true;
			}
		}

		return false;
	}

	public int getUnclaimedRewards(UUID player, ITeamData data)
	{
		int r = 0;

		for (Quest quest : quests)
		{
			r += quest.getUnclaimedRewards(player, data);
		}

		for (QuestChapter chapter : getChildren())
		{
			r += chapter.getUnclaimedRewards(player, data);
		}

		return r;
	}
}